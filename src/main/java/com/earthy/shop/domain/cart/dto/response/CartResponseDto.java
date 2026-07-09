package com.earthy.shop.domain.cart.dto.response;

import java.util.List;

// 장바구니 전체 응답 DTO
public record CartResponseDto(
        List<CartItemResponseDto> items,
        int totalPrice
) {
    public static CartResponseDto from(List<CartItemResponseDto> items) {
        int totalPrice = items.stream()
                .mapToInt(CartItemResponseDto::itemTotalPrice)
                .sum();

        return new CartResponseDto(items, totalPrice);
    }
}