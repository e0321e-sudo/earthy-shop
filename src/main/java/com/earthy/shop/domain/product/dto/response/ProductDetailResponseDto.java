package com.earthy.shop.domain.product.dto.response;

import com.earthy.shop.domain.addon.dto.response.AddonResponseDto;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.entity.ProductSizeOption;
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
        String detailImageUrl,
        String description,
        boolean soldOut,
        List<AddonResponseDto> addons,
        List<ProductSizeOptionResponseDto> sizeOptions
) {
    public static ProductDetailResponseDto of(Product product, List<AddonResponseDto> addons) {
        return of(product, addons, List.of());
    }

    public static ProductDetailResponseDto of(
            Product product,
            List<AddonResponseDto> addons,
            List<ProductSizeOption> sizeOptions
    ) {
        // 재고 기준 품절 여부
        boolean soldOut = product.getCategory() == ProductCategory.POSTER
                ? sizeOptions.stream().noneMatch(option -> option.isActive() && option.getStockQuantity() > 0)
                : product.getStockQuantity() <= 0;

        return new ProductDetailResponseDto(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getCategory().getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDetailImageUrl(),
                product.getDescription(),
                soldOut,
                addons,
                sizeOptions.stream()
                        .map(ProductSizeOptionResponseDto::from)
                        .toList()
        );
    }
}
