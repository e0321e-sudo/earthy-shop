package com.earthy.shop.domain.board.dto.response;

import com.earthy.shop.domain.board.entity.Board;
import com.earthy.shop.domain.board.enums.BoardStatus;
import com.earthy.shop.domain.board.enums.BoardType;
import com.earthy.shop.domain.board.enums.BoardVisibility;

import java.time.LocalDateTime;

// 관리자 게시글 응답 DTO
public record AdminBoardResponseDto(
        Long id,
        Long memberId,
        BoardType type,
        String typeDescription,
        String writerName,
        String writerEmail,
        String title,
        String content,
        BoardVisibility visibility,
        String visibilityDescription,
        BoardStatus status,
        String statusDescription,
        String answer,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminBoardResponseDto from(Board board) {
        return new AdminBoardResponseDto(
                board.getId(),
                board.getMember().getId(),
                board.getType(),
                board.getType().getDescription(),
                board.getMember().getName(),
                board.getMember().getEmail(),
                board.getTitle(),
                board.getContent(),
                board.getVisibility(),
                board.getVisibility().getDescription(),
                board.getStatus(),
                board.getStatus().getDescription(),
                board.getAnswer(),
                board.getAnsweredAt(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}