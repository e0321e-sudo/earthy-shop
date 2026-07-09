package com.earthy.shop.domain.product.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {
    POSTCARD("엽서"),
    POSTER("포스터"),
    ETC("기타");

    private final String description;
}
