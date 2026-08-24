package com.earthy.shop.domain.cart.dto.response;

import com.earthy.shop.domain.cart.entity.CartItemAddon;

// 장바구니 추가상품 응답 DTO
public record CartItemAddonResponseDto(
        Long cartItemAddonId,
        Long addonId,
        String addonName,
        int addonPrice,
        int quantity,
        int totalPrice
) {
    public static CartItemAddonResponseDto from(CartItemAddon cartItemAddon) {
        return new CartItemAddonResponseDto(
                cartItemAddon.getId(),
                cartItemAddon.getAddon().getId(),
                cartItemAddon.getAddonName(),
                cartItemAddon.getAddonPrice(),
                cartItemAddon.getQuantity(),
                cartItemAddon.calculateTotalPrice()
        );
    }
}
