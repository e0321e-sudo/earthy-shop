package com.earthy.shop.domain.notice.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.notice.dto.request.NoticeCreateRequestDto;
import com.earthy.shop.domain.notice.dto.request.NoticeUpdateRequestDto;
import com.earthy.shop.domain.notice.dto.response.NoticeResponseDto;
import com.earthy.shop.domain.notice.enums.NoticeVisibilityFilter;
import com.earthy.shop.domain.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;

    // 관리자 공지사항 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<NoticeResponseDto>>> getAdminNotices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) NoticeVisibilityFilter visibility,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("관리자 공지사항 목록 조회 성공", noticeService.getAdminNotices(keyword, visibility, pageable))
        );
    }

    // 관리자 공지사항 상세 조회
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponseDto<NoticeResponseDto>> getAdminNotice(
            @PathVariable Long noticeId
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("관리자 공지사항 상세 조회 성공", noticeService.getAdminNotice(noticeId))
        );
    }

    // 관리자 공지사항 등록
    @PostMapping
    public ResponseEntity<ApiResponseDto<NoticeResponseDto>> createNotice(
            @Valid @RequestBody NoticeCreateRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("공지사항 등록 성공", noticeService.createNotice(requestDto))
        );
    }

    // 관리자 공지사항 수정
    @PatchMapping("/{noticeId}")
    public ResponseEntity<ApiResponseDto<NoticeResponseDto>> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("공지사항 수정 성공", noticeService.updateNotice(noticeId, requestDto))
        );
    }

    // 관리자 공지사항 비공개 처리
    @PatchMapping("/{noticeId}/hide")
    public ResponseEntity<ApiResponseDto<NoticeResponseDto>> hideNotice(
            @PathVariable Long noticeId
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("공지사항 비공개 처리 성공", noticeService.hideNotice(noticeId))
        );
    }

    // 관리자 공지사항 공개 처리
    @PatchMapping("/{noticeId}/show")
    public ResponseEntity<ApiResponseDto<NoticeResponseDto>> showNotice(
            @PathVariable Long noticeId
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("공지사항 공개 처리 성공", noticeService.showNotice(noticeId))
        );
    }
}