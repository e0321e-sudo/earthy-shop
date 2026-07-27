package com.earthy.shop.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoginProvider {
    LOCAL("일반 로그인"),
    KAKAO("카카오 로그인"),
    NAVER("네이버 로그인");

    private final String description;
}