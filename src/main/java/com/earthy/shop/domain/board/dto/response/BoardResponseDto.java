package com.earthy.shop.domain.board.dto.response;

import com.earthy.shop.domain.board.entity.Board;
import com.earthy.shop.domain.board.enums.BoardType;
import com.earthy.shop.domain.board.enums.BoardVisibility;

import java.time.LocalDateTime;

// 게시글 응답 DTO
public record BoardResponseDto(
        Long id,
        BoardType type,
        String typeDescription,
        String title,
        String content,
        String writerName,
        BoardVisibility visibility,
        String visibilityDescription,
        String answer,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean mine
) {
    public static BoardResponseDto from(Board board) {
        return of(board, false);
    }

    public static BoardResponseDto of(Board board, boolean mine) {
        return new BoardResponseDto(
                board.getId(),
                board.getType(),
                board.getType().getDescription(),
                board.getTitle(),
                board.getContent(),
                maskWriterName(board.getMember().getName()),
                board.getVisibility(),
                board.getVisibility().getDescription(),
                board.getAnswer(),
                board.getAnsweredAt(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                mine
        );
    }

    private static String maskWriterName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return name.charAt(0) + "**";
    }
}
