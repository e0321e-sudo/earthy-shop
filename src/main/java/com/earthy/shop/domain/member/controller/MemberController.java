package com.earthy.shop.domain.member.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.member.dto.request.MemberPasswordUpdateRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberUpdateRequestDto;
import com.earthy.shop.domain.member.dto.response.MemberResponseDto;
import com.earthy.shop.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// 회원 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    // 회원 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<MemberResponseDto>> getMyInfo(
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("회원 정보 조회 성공", memberService.getMyInfo(authentication.getName())));
    }

    // 회원 정보 수정
    @PatchMapping("/me")
    public ResponseEntity<ApiResponseDto<MemberResponseDto>> updateMyInfo(
            Authentication authentication,
            @Valid @RequestBody MemberUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("회원 정보 수정 성공", memberService.updateMyInfo(authentication.getName(), requestDto)));
    }

    // 회원 비밀번호 변경
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponseDto<Void>> updatePassword(
            Authentication authentication,
            @Valid @RequestBody MemberPasswordUpdateRequestDto requestDto
    ) {
        memberService.updatePassword(authentication.getName(), requestDto);

        return ResponseEntity.ok(ApiResponseDto.success("회원 비밀번호 변경 성공", null));
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> deactivateMember(
            Authentication authentication
    ) {
        memberService.deactivateMember(authentication.getName());

        return ResponseEntity.ok(ApiResponseDto.success("회원 탈퇴 성공", null));
    }
}
