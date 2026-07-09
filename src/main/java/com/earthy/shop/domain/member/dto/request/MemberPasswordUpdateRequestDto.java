package com.earthy.shop.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 비밀번호 변경 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberPasswordUpdateRequestDto {

    // 현재 비밀번호
    @NotBlank
    private String currentPassword;

    // 새 비밀번호
    @NotBlank
    private String newPassword;
}
