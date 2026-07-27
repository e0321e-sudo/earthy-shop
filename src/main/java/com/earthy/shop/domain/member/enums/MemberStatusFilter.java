package com.earthy.shop.domain.member.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberStatusFilter {
    ALL("전체"),
    ACTIVE("활성"),
    INACTIVE("비활성");

    private final String description;
}