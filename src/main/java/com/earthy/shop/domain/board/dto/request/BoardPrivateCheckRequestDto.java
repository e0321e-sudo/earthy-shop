package com.earthy.shop.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 비공개 게시글 비밀번호 확인 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardPrivateCheckRequestDto {

    // 게시글 비밀번호
    @NotBlank
    @Size(min = 4, message = "게시글 비밀번호는 최소 4자 이상이어야 합니다.")
    private String postPassword;
}