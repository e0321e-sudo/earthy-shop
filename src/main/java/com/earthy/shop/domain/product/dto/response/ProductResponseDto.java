package com.earthy.shop.domain.product.dto.response;

import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;

// 고객용 상품 응답 DTO
public record ProductResponseDto(
        Long id,
        String name,
        ProductCategory category,
        String categoryDescription,
        int price,
        String imageUrl,
        String description,
        Boolean soldOut
) {
    public static ProductResponseDto from(Product product) {

        // 재고 기준 품절 여부
        boolean soldOut = product.getStockQuantity() <= 0;

        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getCategory().getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDescription(),
                soldOut
        );
    }
}
