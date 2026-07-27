package com.earthy.shop.domain.board.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardVisibility {

    PUBLIC("공개"),
    PRIVATE("비공개");

    private final String description;
}
