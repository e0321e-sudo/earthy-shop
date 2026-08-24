package com.earthy.shop.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlimtalkTemplateCode {
    ORDER_COMPLETED("주문 완료"),
    SHIPPING_STARTED("상품 발송 안내"),
    ORDER_CANCELED_BY_CUSTOMER("주문 취소 완료"),
    ORDER_CANCELED_BY_ADMIN("주문 취소 안내");

    private final String description;
}
