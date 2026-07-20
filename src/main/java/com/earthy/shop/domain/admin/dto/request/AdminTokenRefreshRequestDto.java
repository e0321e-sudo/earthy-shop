package com.earthy.shop.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 토큰 재발급 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminTokenRefreshRequestDto {

    // 리프레시 토큰
    @NotBlank
    private String refreshToken;
}
