package com.earthy.shop.domain.cart.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.cart.dto.request.CartItemAddRequestDto;
import com.earthy.shop.domain.cart.dto.request.CartItemQuantityUpdateRequestDto;
import com.earthy.shop.domain.cart.dto.response.CartResponseDto;
import com.earthy.shop.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// 장바구니 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    // 장바구니 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<CartResponseDto>> getCart(
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(
                "장바구니 조회 성공", cartService.getCart(authentication.getName())));
    }

    // 장바구니 상품 담기
    @PostMapping
    public ResponseEntity<ApiResponseDto<CartResponseDto>> addCartItem(
            Authentication authentication,
            @Valid @RequestBody CartItemAddRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(
                "장바구니 상품 담기 성공", cartService.addCartItem(authentication.getName(), requestDto)));
    }

    // 장바구니 수량 변경
    @PatchMapping("/{cartItemId}")
    public ResponseEntity<ApiResponseDto<CartResponseDto>> updateQuantity(
            Authentication authentication,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemQuantityUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(
                "장바구니 수량 변경 성공", cartService.updateQuantity(authentication.getName(), cartItemId, requestDto)));
    }

    // 장바구니 항목 삭제
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponseDto<CartResponseDto>> deleteCartItem(
            Authentication authentication,
            @PathVariable Long cartItemId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(
                "장바구니 항목 삭제 성공", cartService.deleteCartItem(authentication.getName(), cartItemId)));
    }

    // 장바구니 전체 삭제
    @DeleteMapping
    public ResponseEntity<ApiResponseDto<Void>> clearCart(
            Authentication authentication
    ) {
        cartService.clearCart(authentication.getName());

        return ResponseEntity.ok(ApiResponseDto.success(
                "장바구니 전체 삭제 성공", null));
    }
}