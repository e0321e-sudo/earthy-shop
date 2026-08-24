package com.earthy.shop.domain.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장바구니 추가상품 수량 변경 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemAddonQuantityUpdateRequestDto {

    // 추가상품 수량, 0이면 장바구니에서 제거
    @NotNull
    @Min(value = 0, message = "수량은 0개 이상이어야 합니다.")
    private int quantity;
}
