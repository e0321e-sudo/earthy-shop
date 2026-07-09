package com.earthy.shop.domain.product.dto.response;

import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;

import java.time.LocalDateTime;

// 관리자용 상품 응답 DTO
public record AdminProductResponseDto(
        Long id,
        String name,
        ProductCategory category,
        String categoryDescription,
        int price,
        String imageUrl,
        String description,
        int stockQuantity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminProductResponseDto from(Product product) {
        return new AdminProductResponseDto(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getCategory().getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDescription(),
                product.getStockQuantity(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
