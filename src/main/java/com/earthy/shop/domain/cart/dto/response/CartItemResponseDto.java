package com.earthy.shop.domain.cart.dto.response;

import com.earthy.shop.domain.cart.entity.CartItem;

// 장바구니 상품 응답 DTO
public record CartItemResponseDto(
        Long cartItemId,
        Long productId,
        String productName,
        String productImageUrl,
        int productPrice,
        Long addonId,
        String addonName,
        int addonPrice,
        int quantity,
        int itemTotalPrice
) {
    public static CartItemResponseDto from(CartItem cartItem) {
        int addonPrice = cartItem.getAddon() == null ? 0 : cartItem.getAddon().getPrice();
        Long addonId = cartItem.getAddon() == null ? null : cartItem.getAddon().getId();
        String addonName = cartItem.getAddon() == null ? null : cartItem.getAddon().getName();

        int itemTotalPrice = (cartItem.getProduct().getPrice() + addonPrice) * cartItem.getQuantity();

        return new CartItemResponseDto(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getImageUrl(),
                cartItem.getProduct().getPrice(),
                addonId,
                addonName,
                addonPrice,
                cartItem.getQuantity(),
                itemTotalPrice
        );
    }
}