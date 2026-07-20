package com.earthy.shop.domain.addon.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.addon.dto.request.AddonCreateRequestDto;
import com.earthy.shop.domain.addon.dto.request.AddonUpdateRequestDto;
import com.earthy.shop.domain.addon.dto.response.AdminAddonResponseDto;
import com.earthy.shop.domain.addon.service.AddonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 관리자용 추가상품 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/addons")
public class AdminAddonController {

    private final AddonService addonService;

    // 관리자용 전체 추가상품 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<AdminAddonResponseDto>>> getAdminAddons(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(addonService.getAdminAddons(pageable)));
    }

    // 관리자용 추가상품 등록
    @PostMapping
    public ResponseEntity<ApiResponseDto<AdminAddonResponseDto>> createAddon(
            @RequestBody AddonCreateRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("추가상품 등록 성공", addonService.createAddon(requestDto)));
    }

    // 관리자용 추가상품 수정
    @PatchMapping("/{addonId}")
    public ResponseEntity<ApiResponseDto<AdminAddonResponseDto>> updateAddon(
            @PathVariable Long addonId,
            @RequestBody AddonUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("추가상품 수정 성공", addonService.updateAddon(addonId, requestDto)));
    }

    // 관리자용 추가상품 비활성화
    @PatchMapping("/{addonId}/deactivate")
    public ResponseEntity<ApiResponseDto<AdminAddonResponseDto>> deactivateAddon(
            @PathVariable Long addonId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("추가상품 비활성화 성공", addonService.deactivateAddon(addonId)));
    }

    // 관리자용 추가상품 활성화
    @PatchMapping("/{addonId}/activate")
    public ResponseEntity<ApiResponseDto<AdminAddonResponseDto>> activateAddon(
            @PathVariable Long addonId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("추가상품 활성화 성공", addonService.activateAddon(addonId)));
    }

    // 관리자용 추가상품 삭제
    @DeleteMapping("/{addonId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteAddon(
            @PathVariable Long addonId
    ) {
        addonService.deleteAddon(addonId);
        return ResponseEntity.ok(ApiResponseDto.success("추가상품 삭제 성공", null));
    }
}
