package com.earthy.shop.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 정보 수정 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDto {
    // 회원 이름
    @NotBlank
    private String name;

    // 회원 연락처
    @NotBlank
    @Pattern(
            regexp = "^010-\\d{4}-\\d{4}$",
            message = "전화번호 형식은 010-0000-0000 이어야 합니다."
    )
    private String phone;
}
