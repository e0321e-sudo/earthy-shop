package com.earthy.shop.domain.product.dto.response;

import com.earthy.shop.domain.product.entity.ProductSizeOption;

// 상품 사이즈 옵션 응답 DTO
public record ProductSizeOptionResponseDto(
        Long id,
        String sizeName,
        int additionalPrice,
        int stockQuantity,
        boolean active,
        boolean soldOut
) {
    public static ProductSizeOptionResponseDto from(ProductSizeOption option) {
        return new ProductSizeOptionResponseDto(
                option.getId(),
                option.getSizeName(),
                option.getAdditionalPrice(),
                option.getStockQuantity(),
                option.isActive(),
                option.getStockQuantity() <= 0
        );
    }
}
