package com.earthy.shop.domain.payment.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.domain.payment.dto.request.PaymentConfirmRequestDto;
import com.earthy.shop.domain.payment.dto.response.PaymentResponseDto;
import com.earthy.shop.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 승인
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponseDto<PaymentResponseDto>> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequestDto requestDto
            ) {
        return ResponseEntity.ok(ApiResponseDto.success("결제 승인 성공", paymentService.confirmPayment(requestDto)));
    }
}
