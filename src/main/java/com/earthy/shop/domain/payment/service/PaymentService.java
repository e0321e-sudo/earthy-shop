package com.earthy.shop.domain.payment.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.service.IdempotencyService;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.notification.event.OrderCompletedNotificationEvent;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.service.OrderService;
import com.earthy.shop.domain.payment.client.TossPaymentClient;
import com.earthy.shop.domain.payment.dto.request.PaymentConfirmRequestDto;
import com.earthy.shop.domain.payment.dto.response.PaymentResponseDto;
import com.earthy.shop.domain.payment.dto.toss.TossCancelRequestDto;
import com.earthy.shop.domain.payment.dto.toss.TossConfirmRequestDto;
import com.earthy.shop.domain.payment.dto.toss.TossConfirmResponseDto;
import com.earthy.shop.domain.payment.entity.Payment;
import com.earthy.shop.domain.payment.enums.PaymentStatus;
import com.earthy.shop.domain.payment.repository.PaymentRepository;
import com.earthy.shop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final ProductService productService;
    private final AddonService addonService;
    private final TossPaymentClient tossPaymentClient;
    private final ApplicationEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    private static final DateTimeFormatter ORDER_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 결제 승인
    @Transactional
    public PaymentResponseDto confirmPayment(
            String email,
            PaymentConfirmRequestDto requestDto,
            String idempotencyKey
    ) {
        // 멱등성 키 검증
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        String apiPath = "/api/payments/confirm";

        // 기존 멱등성 키 조회
        IdempotencyKey existingKey = idempotencyService.find(
                email,
                idempotencyKey,
                apiPath
        );

        if (existingKey != null) {
            // 이미 처리 완료된 결제 승인 요청이면 기존 결제 결과 반환
            if (existingKey.isCompleted()) {
                Payment payment = paymentRepository.findById(existingKey.getResourceId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

                return PaymentResponseDto.from(payment);
            }

            // 아직 처리 중이면 중복 요청 차단
            if (existingKey.isProcessing()) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
            }
        }

        // 최초 요청이면 멱등성 키 생성
        IdempotencyKey savedKey = idempotencyService.create(
                email,
                idempotencyKey,
                apiPath
        );

        // 실제 결제 승인
        PaymentResponseDto responseDto = confirmPaymentInternal(requestDto);

        // 결제 승인 완료 기록
        idempotencyService.complete(
                savedKey,
                responseDto.paymentId(),
                "결제 승인 성공"
        );

        return responseDto;
    }

    // 실제 결제 승인
    private PaymentResponseDto confirmPaymentInternal(PaymentConfirmRequestDto requestDto) {
        // 주문 잠금 조회
        Order order = orderService.getOrderForUpdate(requestDto.getOrderId());

        log.info("[PAYMENT CONFIRM STARTED] orderId={} | orderNumber={} | amount={}",
                order.getId(),
                order.getOrderNumber(),
                requestDto.getAmount());

        // 결제 완료 여부 검증
        if (paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE).isPresent()) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // 결제 키 중복 검증
        if (paymentRepository.existsByPaymentKey(requestDto.getPaymentKey())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_KEY);
        }

        // 결제 전 금액 검증
        if (order.getTotalPrice() != requestDto.getAmount()) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // Toss 결제 승인 요청
        TossConfirmResponseDto tossResponse = tossPaymentClient.confirmPayment(
                new TossConfirmRequestDto(
                        requestDto.getPaymentKey(),
                        order.getOrderNumber(),
                        requestDto.getAmount()
                )
        );

        // Toss 결제 상태 검증
        if (!"DONE".equals(tossResponse.status())) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        // Toss 주문번호 검증
        if (!order.getOrderNumber().equals(tossResponse.orderId())) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_MISMATCH);
        }

        // Toss 결제 후 금액 검증
        if (order.getTotalPrice() != tossResponse.totalAmount()) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 주문 상품 재고 차감
        for (OrderItem orderItem : order.getOrderItems()) {
            productService.decreaseStock(orderItem.getProductId(), orderItem.getQuantity());

            if (orderItem.getAddonId() != null) {
                addonService.decreaseStock(orderItem.getAddonId(), orderItem.getAddonQuantity());
            }
        }

        // 결제 생성
        Payment payment = new Payment(
                order,
                tossResponse.paymentKey(),
                tossResponse.orderId(),
                tossResponse.totalAmount(),
                tossResponse.method(),
                PaymentStatus.DONE
        );

        // 주문 결제 완료 처리
        orderService.payOrder(order, tossResponse.method());

        // 결제 저장
        Payment savedPayment = paymentRepository.save(payment);

        log.info("[PAYMENT CONFIRMED] orderId={} | orderNumber={} | paymentKey={} | amount={} | method={}",
                order.getId(),
                order.getOrderNumber(),
                savedPayment.getPaymentKey(),
                savedPayment.getAmount(),
                savedPayment.getMethod());

        // 주문 완료 알림 이벤트 발행
        eventPublisher.publishEvent(new OrderCompletedNotificationEvent(
                order.getReceiverPhone(),
                order.getReceiverName(),
                order.getCreatedAt().format(ORDER_DATE_FORMATTER),
                order.getOrderNumber(),
                createOrderProductName(order),
                order.getTotalPrice()
        ));

        return PaymentResponseDto.from(savedPayment);
    }

    // 주문 상품명 요약
    private String createOrderProductName(Order order) {
        String firstProductName = order.getOrderItems()
                .stream()
                .findFirst()
                .map(OrderItem::getProductName)
                .orElse("EARTHY 상품");

        int extraItemCount = Math.max(order.getOrderItems().size() - 1, 0);

        if (extraItemCount == 0) {
            return firstProductName;
        }

        return firstProductName + " 외 " + extraItemCount + "개";
    }

    // 결제 취소
    @Transactional
    public void cancelPayment(Order order, String cancelReason) {
        // 결제 완료 정보 조회
        Payment payment = paymentRepository.findByOrderAndStatus(order, PaymentStatus.DONE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // Toss 결제 취소 요청
        TossConfirmResponseDto tossResponse = tossPaymentClient.cancelPayment(
                payment.getPaymentKey(),
                new TossCancelRequestDto(cancelReason)
        );

        // Toss 결제 취소 상태 검증
        if (!"CANCELED".equals(tossResponse.status())) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        // 결제 취소 처리
        payment.cancel();

        log.info("[PAYMENT CANCELED] orderId={} | orderNumber={} | paymentKey={}",
                order.getId(),
                order.getOrderNumber(),
                payment.getPaymentKey());
    }
}
