package com.earthy.shop.domain.product.dto.response;

import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.entity.ProductSizeOption;
import com.earthy.shop.domain.product.enums.ProductCategory;

import java.time.LocalDateTime;
import java.util.List;

// 관리자용 상품 응답 DTO
public record AdminProductResponseDto(
        Long id,
        String name,
        ProductCategory category,
        String categoryDescription,
        int price,
        String imageUrl,
        String detailImageUrl,
        String description,
        int stockQuantity,
        List<ProductSizeOptionResponseDto> sizeOptions,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminProductResponseDto from(Product product) {
        return from(product, List.of());
    }

    public static AdminProductResponseDto from(Product product, List<ProductSizeOption> sizeOptions) {
        return new AdminProductResponseDto(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getCategory().getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDetailImageUrl(),
                product.getDescription(),
                product.getStockQuantity(),
                sizeOptions.stream()
                        .map(ProductSizeOptionResponseDto::from)
                        .toList(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
