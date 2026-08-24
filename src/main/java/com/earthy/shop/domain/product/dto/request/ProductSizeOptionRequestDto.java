package com.earthy.shop.domain.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상품 사이즈 옵션 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSizeOptionRequestDto {

    // 기존 옵션 수정 시 사용
    private Long id;

    // 사이즈명
    @NotBlank(message = "사이즈명은 필수입니다.")
    private String sizeName;

    // 추가 금액
    @Min(value = 0, message = "추가 금액은 0원 이상이어야 합니다.")
    private int additionalPrice;

    // 사이즈별 재고 수량
    @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
    private int stockQuantity;

    // 사이즈 옵션 활성 상태
    @NotNull(message = "사이즈 옵션 활성 상태는 필수입니다.")
    private Boolean active;
}
