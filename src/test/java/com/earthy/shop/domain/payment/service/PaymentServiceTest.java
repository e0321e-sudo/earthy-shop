package com.earthy.shop.domain.payment.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.notification.event.OrderCompletedNotificationEvent;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.service.OrderService;
import com.earthy.shop.domain.payment.client.TossPaymentClient;
import com.earthy.shop.domain.payment.dto.request.PaymentConfirmRequestDto;
import com.earthy.shop.domain.payment.dto.response.PaymentResponseDto;
import com.earthy.shop.domain.payment.dto.toss.TossConfirmResponseDto;
import com.earthy.shop.domain.payment.entity.Payment;
import com.earthy.shop.domain.payment.enums.PaymentStatus;
import com.earthy.shop.domain.payment.repository.PaymentRepository;
import com.earthy.shop.domain.product.service.ProductService;
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
    private TossPaymentClient tossPaymentClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void 결제를_승인한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto(1L, "payment-key", 15500, "카드");
        TossConfirmResponseDto tossResponse = new TossConfirmResponseDto(
                "payment-key",
                "ORD-20260728-ABC12345",
                15500,
                "카드",
                "DONE"
        );

        given(orderService.getOrder(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);
        given(tossPaymentClient.confirmPayment(any())).willReturn(tossResponse);
        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    TestEntityUtils.setId(payment, 1L);
                    TestEntityUtils.setField(payment, "createdAt", LocalDateTime.of(2026, 7, 28, 1, 10));
                    return payment;
                });

        // when
        PaymentResponseDto response = paymentService.confirmPayment(requestDto);

        // then
        verify(productService).decreaseStock(1L, 1);
        verify(addonService).decreaseStock(1L, 1);
        verify(orderService).payOrder(order, "카드");
        verify(eventPublisher).publishEvent(any(OrderCompletedNotificationEvent.class));
        assertThat(response.paymentId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(PaymentStatus.DONE);
        assertThat(response.amount()).isEqualTo(15500);
    }

    @Test
    void 이미_완료된_결제가_있으면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = new Payment(order, "old-key", order.getOrderNumber(), 15500, "카드", PaymentStatus.DONE);
        PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto(1L, "payment-key", 15500, "카드");

        given(orderService.getOrder(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_COMPLETED)
                );
        verify(tossPaymentClient, never()).confirmPayment(any());
    }

    @Test
    void 결제_금액이_주문_금액과_다르면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto(1L, "payment-key", 15000, "카드");

        given(orderService.getOrder(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
                );
        verify(tossPaymentClient, never()).confirmPayment(any());
    }

    @Test
    void 토스_승인_상태가_DONE이_아니면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto(1L, "payment-key", 15500, "카드");
        TossConfirmResponseDto tossResponse = new TossConfirmResponseDto(
                "payment-key",
                order.getOrderNumber(),
                15500,
                "카드",
                "FAILED"
        );

        given(orderService.getOrder(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);
        given(tossPaymentClient.confirmPayment(any())).willReturn(tossResponse);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED)
                );
    }

    @Test
    void 결제키가_중복되면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto(1L, "payment-key", 15500, "카드");

        given(orderService.getOrder(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(true);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PAYMENT_KEY)
                );
        verify(tossPaymentClient, never()).confirmPayment(any());
    }

    @Test
    void 토스_주문번호가_주문번호와_다르면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto(1L, "payment-key", 15500, "카드");
        TossConfirmResponseDto tossResponse = new TossConfirmResponseDto(
                "payment-key",
                "ORD-DIFFERENT",
                15500,
                "카드",
                "DONE"
        );

        given(orderService.getOrder(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);
        given(tossPaymentClient.confirmPayment(any())).willReturn(tossResponse);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ORDER_MISMATCH)
                );
    }

    @Test
    void 토스_승인_후_금액이_주문금액과_다르면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto(1L, "payment-key", 15500, "카드");
        TossConfirmResponseDto tossResponse = new TossConfirmResponseDto(
                "payment-key",
                order.getOrderNumber(),
                15000,
                "카드",
                "DONE"
        );

        given(orderService.getOrder(1L)).willReturn(order);
        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.empty());
        given(paymentRepository.existsByPaymentKey("payment-key")).willReturn(false);
        given(tossPaymentClient.confirmPayment(any())).willReturn(tossResponse);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.confirmPayment(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
                );
    }

    @Test
    void 결제를_취소한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = payment(order, PaymentStatus.DONE);
        TossConfirmResponseDto tossResponse = new TossConfirmResponseDto(
                payment.getPaymentKey(),
                order.getOrderNumber(),
                15500,
                "카드",
                "CANCELED"
        );

        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));
        given(tossPaymentClient.cancelPayment(eq("payment-key"), any())).willReturn(tossResponse);

        // when
        paymentService.cancelPayment(order, "고객 요청");

        // then
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
    void 토스_결제취소_상태가_CANCELED가_아니면_예외가_발생한다() {
        // given
        Order order = paidReadyOrder();
        Payment payment = payment(order, PaymentStatus.DONE);
        TossConfirmResponseDto tossResponse = new TossConfirmResponseDto(
                payment.getPaymentKey(),
                order.getOrderNumber(),
                15500,
                "카드",
                "DONE"
        );

        given(paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)).willReturn(Optional.of(payment));
        given(tossPaymentClient.cancelPayment(eq("payment-key"), any())).willReturn(tossResponse);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> paymentService.cancelPayment(order, "고객 요청"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CANCEL_FAILED)
                );
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
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
