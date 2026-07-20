package com.earthy.shop.domain.member.dto.response;

public record MemberLoginResponseDto(
        String accessToken,
        String refreshToken
) {
}
