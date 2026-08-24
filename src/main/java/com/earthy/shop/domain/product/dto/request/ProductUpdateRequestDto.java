package com.earthy.shop.domain.product.dto.request;

import com.earthy.shop.domain.product.enums.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 상품 수정 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequestDto {

    private String name;
    private ProductCategory category;
    private int price;

    @NotBlank(message = "대표 이미지는 필수입니다.")
    private String imageUrl;

    private String detailImageUrl;
    private String description;
    private int stockQuantity;

    @Valid
    private List<ProductSizeOptionRequestDto> sizeOptions;
}
