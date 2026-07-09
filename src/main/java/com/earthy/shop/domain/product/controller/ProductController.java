package com.earthy.shop.domain.product.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.product.dto.response.ProductResponseDto;
import com.earthy.shop.domain.product.enums.ProductCategory;
import com.earthy.shop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // 고객용 상품 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ProductResponseDto>>> getProducts(
            @RequestParam(required = false)ProductCategory category
            ) {
        return ResponseEntity.ok(ApiResponseDto.success(productService.getProducts(category)));
    }

    // 고객용 상품 상세 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> getProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(productService.getProduct(productId)));
    }
}
