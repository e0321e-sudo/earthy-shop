package com.earthy.shop.common.idempotency.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IdempotencyStatus {

    PROCESSING("처리 중"),
    COMPLETED("처리 완료"),
    FAILED("처리 실패");

    private final String description;
}
