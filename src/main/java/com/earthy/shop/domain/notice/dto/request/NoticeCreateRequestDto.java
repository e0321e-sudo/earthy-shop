package com.earthy.shop.domain.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 공지사항 등록 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeCreateRequestDto {

    // 공지사항 제목
    @NotBlank
    private String title;

    // 공지사항 내용
    @NotBlank
    private String content;
}