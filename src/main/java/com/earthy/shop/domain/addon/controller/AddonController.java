package com.earthy.shop.domain.addon.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.addon.dto.response.AddonResponseDto;
import com.earthy.shop.domain.addon.service.AddonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 고객용 추가상품 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/addons")
public class AddonController {

    private final AddonService addonService;

    // 고객용 추가상품 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<AddonResponseDto>>> getAddons() {
        return ResponseEntity.ok(ApiResponseDto.success(addonService.getAddons()));
    }
}