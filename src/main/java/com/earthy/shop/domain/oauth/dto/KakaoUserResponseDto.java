package com.earthy.shop.domain.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 카카오 사용자 정보 응답 DTO
public record KakaoUserResponseDto(

        // 카카오 회원 ID
        Long id,

        // 카카오 계정 정보
        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount,

        // 카카오 프로필 정보
        Properties properties
) {
    public String getEmail() {
        return kakaoAccount == null ? null : kakaoAccount.email();
    }

    public String getNickname() {
        if (properties != null && properties.nickname() != null) {
            return properties.nickname();
        }

        if (kakaoAccount != null && kakaoAccount.profile() != null) {
            return kakaoAccount.profile().nickname();
        }

        return "카카오회원";
    }

    public record KakaoAccount(

            // 카카오 이메일
            String email,

            // 카카오 계정 프로필
            Profile profile
    ) {
    }

    public record Profile(

            // 카카오 닉네임
            String nickname
    ) {
    }

    public record Properties(

            // 카카오 닉네임
            String nickname
    ) {
    }
}