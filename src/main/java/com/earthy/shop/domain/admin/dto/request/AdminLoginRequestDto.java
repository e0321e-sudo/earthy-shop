package com.earthy.shop.domain.admin.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 로그인 요쳥 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequestDto {

    // 관리자 이메일
    @Email
    @NotBlank
    private String email;

    // 관리자 비밀번호
    @NotBlank
    private String password;
}
