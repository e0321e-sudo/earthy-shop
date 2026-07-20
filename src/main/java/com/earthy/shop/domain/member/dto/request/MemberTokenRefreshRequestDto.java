package com.earthy.shop.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 토큰 재발급 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberTokenRefreshRequestDto {

    // 리프레시 토큰
    @NotBlank
    private String refreshToken;
}
