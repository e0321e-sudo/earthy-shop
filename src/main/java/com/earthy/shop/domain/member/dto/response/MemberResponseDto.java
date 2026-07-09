package com.earthy.shop.domain.member.dto.response;

import com.earthy.shop.common.enums.UserRole;
import com.earthy.shop.domain.member.entity.Member;

import java.time.LocalDateTime;

// 회원 응답 DTO
public record MemberResponseDto(
        Long id,
        String email,
        String name,
        String phone,
        UserRole role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MemberResponseDto from(Member member) {
        return new MemberResponseDto(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.getRole(),
                member.isActive(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}