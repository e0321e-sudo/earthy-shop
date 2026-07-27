package com.earthy.shop.domain.notice.dto.response;

import com.earthy.shop.domain.notice.entity.Notice;

import java.time.LocalDateTime;

// 공지사항 응답 DTO
public record NoticeResponseDto(
        Long id,
        String title,
        String content,
        boolean visible,
        String visibleDescription,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeResponseDto from(Notice notice) {
        return new NoticeResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isVisible(),
                notice.isVisible() ? "공개" : "비공개",
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}