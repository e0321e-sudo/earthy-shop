package com.earthy.shop.domain.cart.dto.response;

import com.earthy.shop.domain.cart.entity.CartItem;

import java.util.List;

// 장바구니 상품 응답 DTO
public record CartItemResponseDto(
        Long cartItemId,
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
        List<CartItemAddonResponseDto> addons,
        int quantity,
        int itemTotalPrice
) {
    public static CartItemResponseDto from(CartItem cartItem) {
        int addonPrice = cartItem.getAddon() == null ? 0 : cartItem.getAddon().getPrice();
        Long addonId = cartItem.getAddon() == null ? null : cartItem.getAddon().getId();
        String addonName = cartItem.getAddon() == null ? null : cartItem.getAddon().getName();
        Long sizeOptionId = cartItem.getProductSizeOption() == null ? null : cartItem.getProductSizeOption().getId();
        String sizeName = cartItem.getSelectedSizeName() == null && cartItem.getProductSizeOption() != null
                ? cartItem.getProductSizeOption().getSizeName()
                : cartItem.getSelectedSizeName();
        int productUnitPrice = cartItem.getEffectiveProductUnitPrice();
        int itemTotalPrice = cartItem.calculateItemTotalPrice();
        List<CartItemAddonResponseDto> addons = cartItem.getCartItemAddons()
                .stream()
                .map(CartItemAddonResponseDto::from)
                .toList();

        return new CartItemResponseDto(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getImageUrl(),
                cartItem.getProduct().getPrice(),
                sizeOptionId,
                sizeName,
                cartItem.getSizeAdditionalPrice(),
                productUnitPrice,
                addonId,
                addonName,
                addonPrice,
                cartItem.getAddonQuantity(),
                addons,
                cartItem.getQuantity(),
                itemTotalPrice
        );
    }
}
