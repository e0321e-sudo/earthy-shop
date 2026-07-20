package com.earthy.shop.domain.member.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.member.dto.request.MemberLoginRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberLogoutRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberSignupRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberTokenRefreshRequestDto;
import com.earthy.shop.domain.member.dto.response.MemberLoginResponseDto;
import com.earthy.shop.domain.member.dto.response.MemberResponseDto;
import com.earthy.shop.domain.member.service.MemberAuthService;
import com.earthy.shop.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 회원 인증 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/auth")
public class MemberAuthController {

    private final MemberService memberService;
    private final MemberAuthService memberAuthService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponseDto<MemberResponseDto>> signup(
            @Valid @RequestBody MemberSignupRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("회원가입 성공", memberService.signup(requestDto)));
    }

    // 회원 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<MemberLoginResponseDto>> login(
            @Valid @RequestBody MemberLoginRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("회원 로그인 성공", memberAuthService.login(requestDto)));
    }

    // 회원 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<MemberLoginResponseDto>> refreshToken(
            @Valid @RequestBody MemberTokenRefreshRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("토큰 재발급 성공", memberAuthService.refreshToken(requestDto)));
    }

    // 회원 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @Valid @RequestBody MemberLogoutRequestDto requestDto
    ) {
        memberAuthService.logout(requestDto);

        return ResponseEntity.ok(ApiResponseDto.success("로그아웃 성공", null));
    }
}
