package com.earthy.shop.domain.order.dto.response;

import com.earthy.shop.domain.order.entity.OrderItemAddon;

// 주문 추가상품 응답 DTO
public record OrderItemAddonResponseDto(
        Long orderItemAddonId,
        Long addonId,
        String addonName,
        int addonPrice,
        int quantity,
        int totalPrice
) {
    public static OrderItemAddonResponseDto from(OrderItemAddon orderItemAddon) {
        return new OrderItemAddonResponseDto(
                orderItemAddon.getId(),
                orderItemAddon.getAddonId(),
                orderItemAddon.getAddonName(),
                orderItemAddon.getAddonPrice(),
                orderItemAddon.getQuantity(),
                orderItemAddon.calculateTotalPrice()
        );
    }
}
