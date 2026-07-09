package com.earthy.shop.domain.addon.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AddonType {
    FRAME("액자");

    private final String description;
}
