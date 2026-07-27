package com.earthy.shop.domain.board.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.common.security.UserDetailsImpl;
import com.earthy.shop.domain.board.dto.request.BoardCreateRequestDto;
import com.earthy.shop.domain.board.dto.request.BoardPrivateCheckRequestDto;
import com.earthy.shop.domain.board.dto.request.BoardUpdateRequestDto;
import com.earthy.shop.domain.board.dto.response.BoardListResponseDto;
import com.earthy.shop.domain.board.dto.response.BoardResponseDto;
import com.earthy.shop.domain.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 고객 게시판 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    // 게시글 작성
    @PostMapping
    public ResponseEntity<ApiResponseDto<BoardResponseDto>> createBoard(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody BoardCreateRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("게시글 작성 성공", boardService.createBoard(userDetails.getEmail(), requestDto))
        );
    }

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<BoardListResponseDto>>> getBoards(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("게시글 목록 조회 성공", boardService.getBoards(keyword, pageable))
        );
    }

    // 공개 게시글 상세 조회
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponseDto<BoardResponseDto>> getPublicBoard(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long boardId
    ) {
        String email = userDetails == null ? null : userDetails.getEmail();

        return ResponseEntity.ok(
                ApiResponseDto.success("게시글 상세 조회 성공", boardService.getPublicBoard(email, boardId))
        );
    }

    // 비공개 게시글 비밀번호 확인 후 상세 조회
    @PostMapping("/{boardId}/password")
    public ResponseEntity<ApiResponseDto<BoardResponseDto>> getPrivateBoard(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long boardId,
            @Valid @RequestBody BoardPrivateCheckRequestDto requestDto
    ) {
        String email = userDetails == null ? null : userDetails.getEmail();

        return ResponseEntity.ok(
                ApiResponseDto.success("게시글 상세 조회 성공", boardService.getPrivateBoard(email, boardId, requestDto))
        );
    }

    // 게시글 수정
    @PatchMapping("/{boardId}")
    public ResponseEntity<ApiResponseDto<BoardResponseDto>> updateBoard(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long boardId,
            @Valid @RequestBody BoardUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("게시글 수정 성공", boardService.updateBoard(userDetails.getEmail(), boardId, requestDto))
        );
    }
}
