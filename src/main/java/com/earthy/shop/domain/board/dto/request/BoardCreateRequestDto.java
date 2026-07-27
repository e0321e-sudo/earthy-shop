package com.earthy.shop.domain.board.dto.request;

import com.earthy.shop.domain.board.enums.BoardType;
import com.earthy.shop.domain.board.enums.BoardVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 게시글 작성 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardCreateRequestDto {

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
    @Size(min = 4, max = 20, message = "게시글 비밀번호는 4자 이상 20자 이하이어야 합니다.")
    private String postPassword;
}