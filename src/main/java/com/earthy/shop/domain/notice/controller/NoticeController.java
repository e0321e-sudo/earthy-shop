package com.earthy.shop.domain.notice.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.notice.dto.response.NoticeResponseDto;
import com.earthy.shop.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    // 고객 공지사항 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<NoticeResponseDto>>> getNotices(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("공지사항 목록 조회 성공", noticeService.getNotices(keyword, pageable))
        );
    }

    // 고객 공지사항 상세 조회
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponseDto<NoticeResponseDto>> getNotice(
            @PathVariable Long noticeId
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("공지사항 상세 조회 성공", noticeService.getNotice(noticeId))
        );
    }
}