package com.earthy.shop.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 로그아웃 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminLogoutRequestDto {

    // 리프레시 토큰
    @NotBlank
    private String refreshToken;
}
