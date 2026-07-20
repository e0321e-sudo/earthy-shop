package com.earthy.shop.domain.product.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.product.dto.request.ProductCreateRequestDto;
import com.earthy.shop.domain.product.dto.request.ProductUpdateRequestDto;
import com.earthy.shop.domain.product.dto.response.AdminProductResponseDto;
import com.earthy.shop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    // 관리자용 상품 등록
    @PostMapping
    public ResponseEntity<ApiResponseDto<AdminProductResponseDto>> createProduct(
            @RequestBody ProductCreateRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("상품 등록 성공", productService.createProduct(requestDto)));
    }

    // 관리자용 전체 상품 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<AdminProductResponseDto>>> getAdminProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(productService.getAdminProducts(pageable)));
    }

    // 관리자 상품 수정
    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponseDto<AdminProductResponseDto>> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("상품 수정 성공", productService.updateProduct(productId, requestDto)));
    }

    // 관리자용 상품 비활성화
    @PatchMapping("/{productId}/deactivate")
    public ResponseEntity<ApiResponseDto<AdminProductResponseDto>> deactivateProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("상품 비활성화 성공", productService.deactivateProduct(productId)));
    }

    // 관리자용 상품 활성화
    @PatchMapping("/{productId}/activate")
    public ResponseEntity<ApiResponseDto<AdminProductResponseDto>> activateProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("상품 활성화 성공", productService.activateProduct(productId)));
    }

    // 관리자용 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteProduct(
            @PathVariable Long productId
    ) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponseDto.success("상품 삭제 성공", null));
    }
}
