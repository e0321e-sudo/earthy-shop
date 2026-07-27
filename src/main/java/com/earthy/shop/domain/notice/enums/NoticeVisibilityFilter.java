package com.earthy.shop.domain.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeVisibilityFilter {
    ALL("전체"),
    PUBLIC("공개"),
    PRIVATE("비공개");

    private final String description;
}