package com.earthy.shop.domain.addon.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AddonType {
    FRAME("액자"),
    PREMIUM_FRAME("프리미엄 액자"),
    BASIC_FRAME("베이직 액자");

    private final String description;
}
