package com.earthy.shop.domain.payment.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.service.IdempotencyService;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.notification.event.OrderCompletedNotificationEvent;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.service.OrderService;
import com.earthy.shop.domain.payment.client.PortOnePaymentClient;
import com.earthy.shop.domain.payment.dto.portone.PortOnePaymentResponseDto;
import com.earthy.shop.domain.payment.dto.request.PaymentConfirmRequestDto;
import com.earthy.shop.domain.payment.dto.response.PaymentResponseDto;
import com.earthy.shop.domain.payment.entity.Payment;
import com.earthy.shop.domain.payment.enums.PaymentStatus;
import com.earthy.shop.domain.payment.repository.PaymentRepository;
import com.earthy.shop.domain.product.service.ProductService;
import com.earthy.shop.domain.product.service.ProductSizeOptionService;
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private AddonService addonService;

    @Mock
    private ProductSizeOptionService productSizeOptionService;

    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void 결제를_승인한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);
        PortOnePaymentResponseDto portOneResponse = portOneResponse("payment-key", "ORD-20260728-ABC12345", 15500, "CARD", "PAID");

        given(orderService.getOrderForUpdate(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);
        given(portOnePaymentClient.getPayment(any())).willReturn(portOneResponse);
        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    TestEntityUtils.setId(payment, 1L);
                    TestEntityUtils.setField(payment, "createdAt", LocalDateTime.of(2026, 7, 28, 1, 10));
                    return payment;
                });

        // when
        PaymentResponseDto response = paymentService.confirmPayment("test@example.com", requestDto, "payment-confirm-key");

        // then
        verify(orderService).getOrderForUpdate(1L);
        verify(productService).decreaseStock(1L, 1);
        verify(addonService).decreaseStock(1L, 1);
        verify(orderService).payOrder(order, "카드");
        verify(eventPublisher).publishEvent(any(OrderCompletedNotificationEvent.class));
        assertThat(response.paymentId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(PaymentStatus.DONE);
        assertThat(response.amount()).isEqualTo(15500);
    }

    @Test
    void 완료된_결제승인_요청이면_기존_결제결과를_반환한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = payment(order, PaymentStatus.DONE);
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);
        IdempotencyKey completedKey = new IdempotencyKey(
                "test@example.com",
                "payment-confirm-key",
                "/api/payments/confirm"
        );
        completedKey.complete(1L, "결제 승인 성공");

        given(idempotencyService.find("test@example.com", "payment-confirm-key", "/api/payments/confirm"))
                .willReturn(completedKey);
        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));

        // when
        PaymentResponseDto response = paymentService.confirmPayment(
                "test@example.com",
                requestDto,
                "payment-confirm-key"
        );

        // then
        verify(orderService, never()).getOrderForUpdate(1L);
        verify(portOnePaymentClient, never()).getPayment(any());
        assertThat(response.status()).isEqualTo(PaymentStatus.DONE);
    }

    @Test
    void 처리중인_결제승인_요청이면_중복_요청_예외가_발생한다() {
        // given
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);
        IdempotencyKey processingKey = new IdempotencyKey(
                "test@example.com",
                "payment-confirm-key",
                "/api/payments/confirm"
        );

        given(idempotencyService.find("test@example.com", "payment-confirm-key", "/api/payments/confirm"))
                .willReturn(processingKey);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment(
                        "test@example.com",
                        requestDto,
                        "payment-confirm-key"
                ))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING)
                );
        verify(portOnePaymentClient, never()).getPayment(any());
    }

    @Test
    void 결제승인_멱등성_키가_없으면_예외가_발생한다() {
        // given
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment("test@example.com", requestDto, ""))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED)
                );
    }

    @Test
    void 이미_완료된_결제가_있으면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = new Payment(order, "old-key", order.getOrderNumber(), 15500, "카드", PaymentStatus.DONE);
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);

        given(orderService.getOrderForUpdate(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment("test@example.com", requestDto, "payment-confirm-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_COMPLETED)
                );
        verify(portOnePaymentClient, never()).getPayment(any());
    }

    @Test
    void 결제_금액이_주문_금액과_다르면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = confirmRequest(15000);

        given(orderService.getOrderForUpdate(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment("test@example.com", requestDto, "payment-confirm-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
                );
        verify(portOnePaymentClient, never()).getPayment(any());
    }

    @Test
    void 포트원_결제_상태가_PAID가_아니면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);
        PortOnePaymentResponseDto portOneResponse = portOneResponse("payment-key", order.getOrderNumber(), 15500, "CARD", "FAILED");

        given(orderService.getOrderForUpdate(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);
        given(portOnePaymentClient.getPayment(any())).willReturn(portOneResponse);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment("test@example.com", requestDto, "payment-confirm-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED)
                );
    }

    @Test
    void 결제키가_중복되면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);

        given(orderService.getOrderForUpdate(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(true);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment("test@example.com", requestDto, "payment-confirm-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PAYMENT_KEY)
                );
        verify(portOnePaymentClient, never()).getPayment(any());
    }

    @Test
    void 포트원_주문번호가_주문번호와_다르면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);
        PortOnePaymentResponseDto portOneResponse = portOneResponse("payment-key", "ORD-DIFFERENT", 15500, "CARD", "PAID");

        given(orderService.getOrderForUpdate(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);
        given(portOnePaymentClient.getPayment(any())).willReturn(portOneResponse);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment("test@example.com", requestDto, "payment-confirm-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ORDER_MISMATCH)
                );
    }

    @Test
    void 포트원_결제_금액이_주문금액과_다르면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = confirmRequest(15500);
        PortOnePaymentResponseDto portOneResponse = portOneResponse("payment-key", order.getOrderNumber(), 15000, "CARD", "PAID");

        given(orderService.getOrderForUpdate(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);
        given(portOnePaymentClient.getPayment(any())).willReturn(portOneResponse);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment("test@example.com", requestDto, "payment-confirm-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
                );
    }

    @Test
    void 결제를_취소한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = payment(order, PaymentStatus.DONE);
        PortOnePaymentResponseDto portOneResponse = portOneResponse(payment.getPaymentKey(), order.getOrderNumber(), 15500, "CARD", "CANCELED");

        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));
        given(portOnePaymentClient.cancelPayment(eq("payment-key"), any())).willReturn(portOneResponse);

        // when
        paymentService.cancelPayment(order, "고객 요청");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    void 포트원_취소응답의_cancellation_상태가_성공이면_결제를_취소한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = payment(order, PaymentStatus.DONE);
        PortOnePaymentResponseDto portOneResponse = portOneCancelResponse("SUCCEEDED");

        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));
        given(portOnePaymentClient.cancelPayment(eq("payment-key"), any())).willReturn(portOneResponse);

        // when
        paymentService.cancelPayment(order, "고객 요청");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    void 포트원_취소응답의_최상위_상태가_성공이면_결제를_취소한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = payment(order, PaymentStatus.DONE);
        PortOnePaymentResponseDto portOneResponse = portOneResponse(payment.getPaymentKey(), order.getOrderNumber(), 15500, "CARD", "SUCCEEDED");

        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));
        given(portOnePaymentClient.cancelPayment(eq("payment-key"), any())).willReturn(portOneResponse);

        // when
        paymentService.cancelPayment(order, "고객 요청");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    void 포트원_취소응답이_애매해도_단건조회에서_취소상태면_결제를_취소한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = payment(order, PaymentStatus.DONE);
        PortOnePaymentResponseDto cancelResponse = portOneResponse(payment.getPaymentKey(), order.getOrderNumber(), 15500, "CARD", "PAID");
        PortOnePaymentResponseDto verifiedResponse = portOneResponse(payment.getPaymentKey(), order.getOrderNumber(), 15500, "CARD", "CANCELED");

        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));
        given(portOnePaymentClient.cancelPayment(eq("payment-key"), any())).willReturn(cancelResponse);
        given(portOnePaymentClient.getPaymentForCancelVerification("payment-key")).willReturn(verifiedResponse);

        // when
        paymentService.cancelPayment(order, "고객 요청");

        // then
        verify(portOnePaymentClient).getPaymentForCancelVerification("payment-key");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    void 결제취소_대상_결제가_없으면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();

        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.cancelPayment(order, "고객 요청"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND)
                );
    }

    @Test
    void 포트원_결제취소_상태가_CANCELED가_아니면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = payment(order, PaymentStatus.DONE);
        PortOnePaymentResponseDto portOneResponse = portOneResponse(payment.getPaymentKey(), order.getOrderNumber(), 15500, "CARD", "PAID");

        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));
        given(portOnePaymentClient.cancelPayment(eq("payment-key"), any())).willReturn(portOneResponse);
        given(portOnePaymentClient.getPaymentForCancelVerification("payment-key")).willReturn(portOneResponse);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.cancelPayment(order, "고객 요청"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CANCEL_FAILED)
                );
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
    }

    private PaymentConfirmRequestDto confirmRequest(int amount) {
        return new PaymentConfirmRequestDto(1L, "payment-key", null, amount, "카드");
    }

    private PortOnePaymentResponseDto portOneResponse(
            String paymentId,
            String orderName,
            int amount,
            String method,
            String status
    ) {
        return new PortOnePaymentResponseDto(
                paymentId,
                paymentId,
                null,
                status,
                orderName,
                method,
                new PortOnePaymentResponseDto.Amount(amount),
                new PortOnePaymentResponseDto.Method(method, null),
                null,
                null
        );
    }

    private PortOnePaymentResponseDto portOneCancelResponse(String cancellationStatus) {
        return new PortOnePaymentResponseDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new PortOnePaymentResponseDto.Cancellation(cancellationStatus)
        );
    }

    private Order paidReadyOrder() {
        Member member = new Member("test@example.com", "encodedPassword", "홍길동", "010-1234-5678");
        TestEntityUtils.setId(member, 1L);

        Order order = new Order(
                "ORD-20260728-ABC12345",
                member,
                "홍길동",
                "010-1234-5678",
                "51100",
                "경남 창원시 소답동",
                "711호",
                "문 앞에 놓아주세요",
                13000,
                2500,
                0
        );
        TestEntityUtils.setId(order, 1L);
        TestEntityUtils.setField(order, "createdAt", LocalDateTime.of(2026, 7, 28, 1, 0));
        order.addOrderItem(new OrderItem(
                1L,
                "sunset sea poster",
                "/assets/products/sunset-sea.jpeg",
                3500,
                1L,
                "A3 원목 액자",
                9500,
                1,
                1
        ));

        return order;
    }

    private Payment payment(Order order, PaymentStatus status) {
        Payment payment = new Payment(
                order,
                "payment-key",
                order.getOrderNumber(),
                15500,
                "카드",
                status
        );
        TestEntityUtils.setId(payment, 1L);
        TestEntityUtils.setField(payment, "createdAt", LocalDateTime.of(2026, 7, 28, 1, 10));
        return payment;
    }
}
