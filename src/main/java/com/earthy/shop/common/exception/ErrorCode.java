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

    // 재고
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "OUT_OF_STOCK", "재고가 부족합니다."),

    // 관리자
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "관리자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),

    // 멤버
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "SAME_AS_OLD_PASSWORD", "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),

    // 장바구니
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "장바구니 상품을 찾을 수 없습니다."),
    EMPTY_CART(HttpStatus.BAD_REQUEST, "EMPTY_CART", "장바구니가 비어있습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "수량은 1개 이상이어야 합니다."),

    // 주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."),

    // 결제
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PAYMENT_ALREADY_COMPLETED", "이미 결제가 완료된 주문입니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_CONFIRM_FAILED", "결제 승인에 실패했습니다."),
    DUPLICATE_PAYMENT_KEY(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT_KEY", "이미 사용된 결제 키입니다."),
    PAYMENT_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_ORDER_MISMATCH", "주문번호가 일치하지 않습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH", "결제 금액이 주문 금액과 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}