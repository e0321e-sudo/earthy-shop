package com.earthy.shop.domain.board.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.board.dto.request.BoardAnswerRequestDto;
import com.earthy.shop.domain.board.dto.response.AdminBoardResponseDto;
import com.earthy.shop.domain.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 관리자 게시판 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
public class AdminBoardController {

    private final BoardService boardService;

    // 관리자 게시글 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<AdminBoardResponseDto>>> getAdminBoards(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("관리자 게시글 목록 조회 성공", boardService.getAdminBoards(keyword, pageable))
        );
    }

    // 관리자 게시글 상세 조회
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponseDto<AdminBoardResponseDto>> getAdminBoard(
            @PathVariable Long boardId
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("관리자 게시글 상세 조회 성공", boardService.getAdminBoard(boardId))
        );
    }

    // 관리자 답변 등록
    @PatchMapping("/{boardId}/answer")
    public ResponseEntity<ApiResponseDto<AdminBoardResponseDto>> answerBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardAnswerRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("관리자 답변 등록 성공", boardService.answerBoard(boardId, requestDto))
        );
    }
}