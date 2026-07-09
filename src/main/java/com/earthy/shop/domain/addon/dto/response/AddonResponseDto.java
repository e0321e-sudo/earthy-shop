package com.earthy.shop.domain.addon.dto.response;

import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.addon.enums.AddonType;

// 고객용 추가상품 응답 DTO
public record AddonResponseDto(
        Long id,
        String name,
        AddonType type,
        String typeDescription,
        int price,
        boolean soldOut
) {
    public static AddonResponseDto from(Addon addon) {
        // 재고 기준 품절 여부
        boolean soldOut = addon.getStockQuantity() <= 0;

        return new AddonResponseDto(
                addon.getId(),
                addon.getName(),
                addon.getType(),
                addon.getType().getDescription(),
                addon.getPrice(),
                soldOut
        );
    }
}