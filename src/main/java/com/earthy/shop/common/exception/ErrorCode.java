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
    ADDON_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDON_NOT_FOUND", "해당 추가상품을 찾을 수 없습니다."),

    // 관리자
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "관리자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),

    // 멤버
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "SAME_AS_OLD_PASSWORD", "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}