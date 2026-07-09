package com.earthy.shop.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 로그인 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberLoginRequestDto {

    // 회원 이메일
    @Email
    @NotBlank
    private String email;

    // 회원 비밀번호
    @NotBlank
    private String password;
}