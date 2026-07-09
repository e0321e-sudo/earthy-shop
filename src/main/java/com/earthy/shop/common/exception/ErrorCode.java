package com.earthy.shop.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "해당 상품을 찾을 수 없습니다."),

    // 추가상품
    ADDON_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDON_NOT_FOUND", "해당 추가상품을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}