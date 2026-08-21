package com.earthy.shop.domain.member.dto.response;

import com.earthy.shop.common.enums.LoginProvider;
import com.earthy.shop.domain.member.entity.Member;

// 이메일 찾기 응답 DTO
public record MemberEmailFindResponseDto(
        String email,
        LoginProvider provider,
        String providerDescription
) {
    public static MemberEmailFindResponseDto from(Member member) {
        LoginProvider provider = member.getProvider() == null
                ? LoginProvider.LOCAL
                : member.getProvider();

        return new MemberEmailFindResponseDto(
                member.getEmail(),
                provider,
                provider.getDescription()
        );
    }
}
