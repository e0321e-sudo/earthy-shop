package com.earthy.shop.domain.payment.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.addon.service.AddonService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final ProductService productService;
    private final AddonService addonService;
    private final TossPaymentClient tossPaymentClient;

    // 결제 승인
    @Transactional
    public PaymentResponseDto confirmPayment(PaymentConfirmRequestDto requestDto) {
        // 주문 조회
        Order order = orderService.getOrder(requestDto.getOrderId());

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

        return PaymentResponseDto.from(savedPayment);
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
    }
}
