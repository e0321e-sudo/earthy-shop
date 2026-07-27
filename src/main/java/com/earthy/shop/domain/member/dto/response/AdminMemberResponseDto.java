package com.earthy.shop.domain.member.dto.response;

import com.earthy.shop.domain.member.entity.Member;

import java.time.LocalDateTime;

// 관리자 화원 응답 DTO
public record AdminMemberResponseDto(
   Long id,
   String email,
   String name,
   String phone,
   boolean active,
   LocalDateTime createdAt,
   LocalDateTime updatedAt
) {
    public static AdminMemberResponseDto from(Member member) {
        return new AdminMemberResponseDto(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.isActive(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
