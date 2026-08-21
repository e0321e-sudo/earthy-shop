package com.earthy.shop.domain.order.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.service.IdempotencyService;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.payment.service.PaymentService;
import com.earthy.shop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderCancelService {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ProductService productService;
    private final AddonService addonService;
    private final IdempotencyService idempotencyService;

    // 내 주문 취소
    @Transactional
    public OrderResponseDto cancelMyOrder(
            String email,
            Long orderId,
            String cancelReason,
            String idempotencyKey
    ) {
        // 멱등성 키 검증
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        String apiPath = "/api/orders/" + orderId + "/cancel";

        // 기존 멱등성 키 조회
        IdempotencyKey existingKey = idempotencyService.find(email, idempotencyKey, apiPath);

        if (existingKey != null) {
            // 이미 처리 완료된 취소 요청이면 기존 주문 결과 반환
            if (existingKey.isCompleted()) {
                return orderService.getMyOrder(email, existingKey.getResourceId());
            }

            // 아직 처리 중이면 중복 요청 차단
            if (existingKey.isProcessing()) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
            }
        }

        // 최초 요청이면 멱등성 키 생성
        IdempotencyKey savedKey = idempotencyService.create(email, idempotencyKey, apiPath);

        // 내 주문 잠금 조회
        Order order = orderService.findMyOrderForUpdate(email, orderId);

        // 취소 사유 기본값 처리
        String reason = cancelReason == null || cancelReason.isBlank()
                ? "고객 요청으로 인한 주문 취소"
                : cancelReason;

        // 주문 취소 처리
        cancelOrder(order, reason);

        // 주문 취소 완료 기록
        idempotencyService.complete(
                savedKey,
                order.getId(),
                "주문 취소 성공"
        );

        return OrderResponseDto.from(order);
    }

    // 관리자 주문 취소
    @Transactional
    public OrderResponseDto cancelAdminOrder(Long orderId, String cancelReason, String idempotencyKey) {
        // 멱등성 키 검증
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        String adminKeyOwner = "ADMIN";
        String apiPath = "/api/admin/orders/" + orderId + "/cancel";

        // 기존 멱등성 키 조회
        IdempotencyKey existingKey = idempotencyService.find(adminKeyOwner, idempotencyKey, apiPath);

        if (existingKey != null) {
            // 이미 처리 완료된 취소 요청이면 기존 주문 결과 반환
            if (existingKey.isCompleted()) {
                return orderService.getOrderDetail(existingKey.getResourceId());
            }

            // 아직 처리 중이면 중복 요청 차단
            if (existingKey.isProcessing()) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
            }
        }

        // 최초 요청이면 멱등성 키 생성
        IdempotencyKey savedKey = idempotencyService.create(adminKeyOwner, idempotencyKey, apiPath);

        // 주문 잠금 조회
        Order order = orderService.getOrderForUpdate(orderId);

        // 취소 사유 기본값 처리
        String reason = cancelReason == null || cancelReason.isBlank()
                ? "관리자 요청으로 인한 주문 취소"
                : cancelReason;

        // 주문 취소 처리
        cancelOrder(order, reason);

        // 관리자 주문 취소 완료 기록
        idempotencyService.complete(
                savedKey,
                order.getId(),
                "관리자 주문 취소 성공"
        );

        return OrderResponseDto.from(order);
    }

    // 주문 취소 처리
    private void cancelOrder(Order order, String cancelReason) {
        // 결제 완료 주문 취소 처리
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PREPARING) {
            paymentService.cancelPayment(order, cancelReason);
            restoreStock(order);
        }

        // 주문 상태 취소 처리
        order.cancel();

        log.info("[ORDER CANCELED] orderId={} | orderNumber={} | status={}",
                order.getId(),
                order.getOrderNumber(),
                order.getStatus());
    }

    // 주문 상품 재고 복구
    private void restoreStock(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            productService.increaseStock(orderItem.getProductId(), orderItem.getQuantity());

            if (orderItem.getAddonId() != null) {
                addonService.increaseStock(orderItem.getAddonId(), orderItem.getAddonQuantity());
            }
        }
    }
}
