package com.earthy.shop.domain.order.service;

import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.payment.service.PaymentService;
import com.earthy.shop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderCancelService {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ProductService productService;
    private final AddonService addonService;

    // 내 주문 취소
    @Transactional
    public OrderResponseDto cancelMyOrder(String email, Long orderId, String cancelReason) {
        // 내 주문 조회
        Order order = orderService.findMyOrder(email, orderId);

        // 취소 사유 기본값 처리
        String reason = cancelReason == null || cancelReason.isBlank()
                ? "고객 요청으로 인한 주문 취소"
                : cancelReason;

        // 주문 취소 처리
        cancelOrder(order, reason);

        return OrderResponseDto.from(order);
    }

    // 관리자 주문 취소
    @Transactional
    public OrderResponseDto cancelAdminOrder(Long orderId, String cancelReason) {
        // 주문 조회
        Order order = orderService.getOrder(orderId);

        // 취소 사유 기본값 처리
        String reason = cancelReason == null || cancelReason.isBlank()
                ? "관리자 요청으로 인한 주문 취소"
                : cancelReason;

        // 주문 취소 처리
        cancelOrder(order, reason);

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
