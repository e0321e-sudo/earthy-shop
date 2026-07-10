package com.earthy.shop.domain.order.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    // 관리자 주문 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OrderResponseDto>>> getOrders() {
        return ResponseEntity.ok(ApiResponseDto.success("관리자 주문 목록 조회 성공", orderService.getOrders()));
    }

    // 관리자 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> getOrderDetail(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success("관리자 주문 상세 조회 성공", orderService.getOrderDetail(orderId)));
    }

    // 관리자 주문 상태 변경
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequestDto requestDto
            ) {
        return ResponseEntity.ok(ApiResponseDto.success("관리자 주문 상태 변경 성공", orderService.updateOrderStatus(orderId, requestDto)));
    }
}
