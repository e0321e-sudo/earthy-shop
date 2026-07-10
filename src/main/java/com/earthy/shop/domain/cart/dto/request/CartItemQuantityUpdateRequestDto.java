package com.earthy.shop.domain.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장바구니 수량 변경 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemQuantityUpdateRequestDto {

    // 수량
    @NotNull
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private int quantity;

    // 추가상품 수량
    private Integer addonQuantity;
}
