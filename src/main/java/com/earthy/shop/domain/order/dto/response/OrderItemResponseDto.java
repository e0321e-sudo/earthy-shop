package com.earthy.shop.domain.order.dto.response;

import com.earthy.shop.domain.order.entity.OrderItem;

public record OrderItemResponseDto(
        Long orderItemId,
        Long productId,
        String productName,
        String productImageUrl,
        int productPrice,
        Long addonId,
        String addonName,
        int addonPrice,
        int addonQuantity,
        int quantity,
        int itemTotalPrice
) {
    public static OrderItemResponseDto from(OrderItem orderItem) {
        return new OrderItemResponseDto(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getProductImageUrl(),
                orderItem.getProductPrice(),
                orderItem.getAddonId(),
                orderItem.getAddonName(),
                orderItem.getAddonPrice(),
                orderItem.getAddonQuantity(),
                orderItem.getQuantity(),
                orderItem.getItemTotalPrice()
        );
    }
}
