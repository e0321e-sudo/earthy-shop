package com.earthy.shop.domain.addon.dto.request;

import com.earthy.shop.domain.addon.enums.AddonType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 추가상품 수정 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddonUpdateRequestDto {

    private String name;
    private AddonType type;
    private int price;
    private int stockQuantity;
}