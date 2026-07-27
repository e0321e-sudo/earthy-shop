package com.earthy.shop.domain.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 카카오 토큰 응답 DTO
public record KakaoTokenResponseDto(

        // 카카오 액세스 토큰
        @JsonProperty("access_token")
        String accessToken,

        // 카카오 리프레시 토큰
        @JsonProperty("refresh_token")
        String refreshToken,

        // 토큰 타입
        @JsonProperty("token_type")
        String tokenType,

        // 액세스 토큰 만료 시간
        @JsonProperty("expires_in")
        Integer expiresIn
) {
}