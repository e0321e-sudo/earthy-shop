package com.earthy.shop.domain.order.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.common.security.UserDetailsImpl;
import com.earthy.shop.domain.order.dto.request.OrderCancelRequestDto;
import com.earthy.shop.domain.order.dto.request.OrderCreateRequestDto;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.service.OrderCancelService;
import com.earthy.shop.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderCancelService orderCancelService;

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
    public ResponseEntity<ApiResponseDto<PageResponseDto<OrderResponseDto>>> getMyOrders(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(ApiResponseDto.success("내 주문 목록 조회 성공", orderService.getMyOrders(email, pageable)));
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

    // 내 주문 취소
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> cancelMyOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId,
            @RequestBody OrderCancelRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(
                "주문 취소 성공",
                orderCancelService.cancelMyOrder(
                        userDetails.getUsername(),
                        orderId,
                        requestDto.getCancelReason()
                )
        ));
    }
}
