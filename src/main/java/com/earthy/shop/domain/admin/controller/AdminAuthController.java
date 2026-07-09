package com.earthy.shop.domain.admin.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.admin.dto.request.AdminLoginRequestDto;
import com.earthy.shop.domain.admin.dto.response.AdminLoginResponseDto;
import com.earthy.shop.domain.admin.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 인증 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    // 관리자 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AdminLoginResponseDto>> login(
            @Valid @RequestBody AdminLoginRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("관리자 로그인 성공", adminAuthService.login(requestDto)));
    }
}
