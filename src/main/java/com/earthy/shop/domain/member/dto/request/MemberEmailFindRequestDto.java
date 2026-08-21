package com.earthy.shop.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 이메일 찾기 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberEmailFindRequestDto {

    // 회원 이름
    @NotBlank
    private String name;

    // 회원 연락처
    @NotBlank
    private String phone;
}
