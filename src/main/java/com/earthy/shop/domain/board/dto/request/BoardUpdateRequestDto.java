package com.earthy.shop.domain.board.dto.request;

import com.earthy.shop.domain.board.enums.BoardType;
import com.earthy.shop.domain.board.enums.BoardVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 게시글 수정 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateRequestDto {

    // 문의 종류
    @NotNull
    private BoardType type;

    // 게시글 제목
    @NotBlank
    private String title;

    // 게시글 내용
    @NotBlank
    private String content;

    // 공개 여부
    @NotNull
    private BoardVisibility visibility;

    // 비공개 게시글 비밀번호
    private String postPassword;
}