package com.earthy.shop.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    ADMIN("관리자"),
    MEMBER("회원");

    private final String description;
}
