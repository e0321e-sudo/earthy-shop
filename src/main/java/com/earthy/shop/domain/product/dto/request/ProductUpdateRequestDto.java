package com.earthy.shop.domain.product.dto.request;

import com.earthy.shop.domain.product.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상품 수정 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequestDto {

    private String name;
    private ProductCategory category;
    private int price;
    private String imageUrl;
    private String description;
    private int stockQuantity;
}
