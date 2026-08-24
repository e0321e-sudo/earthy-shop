package com.earthy.shop.domain.order.dto.response;

import com.earthy.shop.domain.order.entity.OrderItem;

import java.util.List;

public record OrderItemResponseDto(
        Long orderItemId,
        Long productId,
        String productName,
        String productImageUrl,
        int productPrice,
        Long sizeOptionId,
        String sizeName,
        int sizeAdditionalPrice,
        int productUnitPrice,
        Long addonId,
        String addonName,
        int addonPrice,
        int addonQuantity,
        List<OrderItemAddonResponseDto> addons,
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
                orderItem.getSizeOptionId(),
                orderItem.getSizeName(),
                orderItem.getSizeAdditionalPrice(),
                orderItem.getProductUnitPrice(),
                orderItem.getAddonId(),
                orderItem.getAddonName(),
                orderItem.getAddonPrice(),
                orderItem.getAddonQuantity(),
                orderItem.getOrderItemAddons()
                        .stream()
                        .map(OrderItemAddonResponseDto::from)
                        .toList(),
                orderItem.getQuantity(),
                orderItem.getItemTotalPrice()
        );
    }
}
