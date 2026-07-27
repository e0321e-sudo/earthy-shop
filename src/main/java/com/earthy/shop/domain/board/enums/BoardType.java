package com.earthy.shop.domain.board.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardType {
    PRODUCT("상품문의"),
    DELIVERY("배송문의"),
    EXCHANGE_RETURN("교환/반품문의"),
    CANCEL_CHANGE("배송 전 취소/변경"),
    PAYMENT("입금확인 문의"),
    ETC("기타문의");

    private final String description;
}