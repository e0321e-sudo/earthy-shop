package com.earthy.shop.domain.cart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 장바구니 상품 담기 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemAddRequestDto {

    // 상품 ID
    @NotNull
    private Long productId;

    // 포스터 사이즈 옵션 ID
    private Long productSizeOptionId;

    // 추가상품 ID
    private Long addonId;

    // 추가상품 수량
    private Integer addonQuantity;

    // 복수 추가상품 목록
    private List<@Valid CartItemAddonRequestDto> addons;

    // 수량
    @NotNull
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private int quantity;

    public CartItemAddRequestDto(
            Long productId,
            Long productSizeOptionId,
            Long addonId,
            Integer addonQuantity,
            int quantity
    ) {
        this.productId = productId;
        this.productSizeOptionId = productSizeOptionId;
        this.addonId = addonId;
        this.addonQuantity = addonQuantity;
        this.quantity = quantity;
    }
}
