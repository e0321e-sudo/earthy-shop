package com.earthy.shop.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 비밀번호 찾기 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberPasswordFindRequestDto {

    // 회원 이메일
    @Email
    @NotBlank
    private String email;
}
