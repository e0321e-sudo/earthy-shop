package com.earthy.shop.domain.member.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.member.dto.response.AdminMemberResponseDto;
import com.earthy.shop.domain.member.enums.MemberStatusFilter;
import com.earthy.shop.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    // 관리자 회원 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<AdminMemberResponseDto>>> getAdminMembers(
            @RequestParam(required = false) MemberStatusFilter status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("관리자 회원 목록 조회 성공", memberService.getAdminMembers(status, pageable))
        );
    }

    // 관리자 회원 상세 조회
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponseDto> getAdminMember(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success("관리자 회원 상세 조회 성공", memberService.getAdminMember(memberId))
        );
    }
}
