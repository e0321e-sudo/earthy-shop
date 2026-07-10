package com.earthy.shop.domain.order.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.order.dto.request.OrderCreateRequestDto;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> createOrder(
            Authentication authentication,
            @Valid @RequestBody OrderCreateRequestDto requestDto
            ) {
        String email = authentication.getName();

        return ResponseEntity.ok(ApiResponseDto.success("주문 생성 성공", orderService.createOrder(email, requestDto)));
    }

    // 내 주문 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OrderResponseDto>>> getMyOrders(
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(ApiResponseDto.success("내 주문 목록 조회 성공", orderService.getMyOrders(email)));
    }

    // 내 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> getMyOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(ApiResponseDto.success("내 주문 상세 조회 성공", orderService.getMyOrder(email,orderId)));
    }
}
