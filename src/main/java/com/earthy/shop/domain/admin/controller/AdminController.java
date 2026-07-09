package com.earthy.shop.domain.admin.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.admin.dto.request.AdminPasswordUpdateRequestDto;
import com.earthy.shop.domain.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// 관리자 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    // 관리자 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<ApiResponseDto<Void>> updatePassword(
            Authentication authentication,
            @Valid @RequestBody AdminPasswordUpdateRequestDto requestDto
            ) {
        adminService.updatePassword(authentication.getName(), requestDto);

        return ResponseEntity.ok(ApiResponseDto.success("관리자 비밀번호 변경 성공", null));
    }
}
