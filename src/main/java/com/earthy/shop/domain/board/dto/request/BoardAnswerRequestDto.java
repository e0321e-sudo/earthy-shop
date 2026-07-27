package com.earthy.shop.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 게시글 답변 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardAnswerRequestDto {

    // 관리자 답변
    @NotBlank
    private String answer;
}