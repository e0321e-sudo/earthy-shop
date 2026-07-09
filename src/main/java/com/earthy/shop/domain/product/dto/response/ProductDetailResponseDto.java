package com.earthy.shop.domain.product.dto.response;

import com.earthy.shop.domain.addon.dto.response.AddonResponseDto;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;

import java.util.List;

// 고객용 상품 상세 응답 DTO
public record ProductDetailResponseDto(
        Long id,
        String name,
        ProductCategory category,
        String categoryDescription,
        int price,
        String imageUrl,
        String description,
        boolean soldOut,
        List<AddonResponseDto> addons
) {
    public static ProductDetailResponseDto of(Product product, List<AddonResponseDto> addons) {
        // 재고 기준 품절 여부
        boolean soldOut = product.getStockQuantity() <= 0;

        return new ProductDetailResponseDto(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getCategory().getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDescription(),
                soldOut,
                addons
        );
    }
}