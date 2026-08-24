package com.earthy.shop.domain.order.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.service.IdempotencyService;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.notification.enums.OrderCancelRequester;
import com.earthy.shop.domain.notification.event.OrderCanceledNotificationEvent;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.entity.OrderItemAddon;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.payment.service.PaymentService;
import com.earthy.shop.domain.product.service.ProductService;
import com.earthy.shop.domain.product.service.ProductSizeOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ProductSizeOptionService productSizeOptionService;
    private final AddonService addonService;
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher eventPublisher;

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

        // 고객 주문 취소는 결제 완료 후 배송 전 상태에서만 허용
        validateCustomerCancelable(order);

        // 취소 사유 필수 검증
        String reason = validateCancelReason(cancelReason);

        // 주문 취소 처리
        cancelOrder(order, reason);

        // 주문 취소 알림 이벤트 발행
        publishOrderCanceledEvent(order, reason, OrderCancelRequester.CUSTOMER);

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

        // 취소 사유 필수 검증
        String reason = validateCancelReason(cancelReason);

        // 주문 취소 처리
        cancelOrder(order, reason);

        // 관리자 주문 취소 알림 이벤트 발행
        publishOrderCanceledEvent(order, reason, OrderCancelRequester.ADMIN);

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
        order.cancel(cancelReason);

        log.info("[ORDER CANCELED] orderId={} | orderNumber={} | status={}",
                order.getId(),
                order.getOrderNumber(),
                order.getStatus());
    }

    // 주문 취소 알림 이벤트 발행
    private void publishOrderCanceledEvent(Order order, String cancelReason, OrderCancelRequester requester) {
        eventPublisher.publishEvent(new OrderCanceledNotificationEvent(
                order.getReceiverPhone(),
                order.getOrderNumber(),
                order.getTotalPrice(),
                cancelReason,
                requester
        ));
    }

    // 취소 사유 필수 검증
    private String validateCancelReason(String cancelReason) {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_REASON_REQUIRED);
        }

        return cancelReason.trim();
    }

    // 고객 취소 가능 상태 검증
    private void validateCustomerCancelable(Order order) {
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PREPARING) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE);
        }
    }

    // 주문 상품 재고 복구
    private void restoreStock(Order order) {
        log.info("[ORDER STOCK RESTORE STARTED] orderId={} | orderNumber={}",
                order.getId(),
                order.getOrderNumber());

        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getSizeOptionId() != null) {
                productSizeOptionService.increaseStock(orderItem.getSizeOptionId(), orderItem.getQuantity());
            } else {
                productService.increaseStock(orderItem.getProductId(), orderItem.getQuantity());
            }

            // 구형 단일 addon 주문은 복구하되, 복수 addon snapshot이 있으면 중복 복구하지 않는다.
            if (orderItem.getAddonId() != null && orderItem.getOrderItemAddons().isEmpty()) {
                addonService.increaseStock(orderItem.getAddonId(), orderItem.getAddonQuantity());
            }

            for (OrderItemAddon addon : orderItem.getOrderItemAddons()) {
                addonService.increaseStock(addon.getAddonId(), addon.getQuantity());
            }
        }

        log.info("[ORDER STOCK RESTORED] orderId={} | orderNumber={}",
                order.getId(),
                order.getOrderNumber());
    }
}
