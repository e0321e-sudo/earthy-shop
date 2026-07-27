package com.earthy.shop.domain.board.dto.response;

import com.earthy.shop.domain.board.entity.Board;
import com.earthy.shop.domain.board.enums.BoardType;
import com.earthy.shop.domain.board.enums.BoardVisibility;

import java.time.LocalDateTime;

// 게시글 목록 응답 DTO
public record BoardListResponseDto(
        Long id,
        BoardType type,
        String typeDescription,
        String title,
        String writerName,
        BoardVisibility visibility,
        String visibilityDescription,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BoardListResponseDto from(Board board) {
        return new BoardListResponseDto(
                board.getId(),
                board.getType(),
                board.getType().getDescription(),
                board.getTitle(),
                maskWriterName(board.getMember().getName()),
                board.getVisibility(),
                board.getVisibility().getDescription(),
                board.getAnsweredAt(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }

    private static String maskWriterName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return name.charAt(0) + "**";
    }
}
