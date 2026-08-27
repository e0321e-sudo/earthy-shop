package com.earthy.shop.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp = "^\\d{3}-\\d{4}-\\d{4}$",
            message = "연락처 형식을 확인해주세요."
    )
    private String phone;
}
