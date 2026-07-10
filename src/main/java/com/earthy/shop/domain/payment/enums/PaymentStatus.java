package com.earthy.shop.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    READY("결제 준비"),
    DONE("결제 완료"),
    CANCELED("결제 취소"),
    FAILED("결제 실패");

    private final String description;
}
