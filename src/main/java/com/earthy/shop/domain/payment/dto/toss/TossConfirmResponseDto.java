package com.earthy.shop.domain.payment.dto.toss;

public record TossConfirmResponseDto(
        String paymentKey,
        String orderId,
        int totalAmount,
        String method,
        String status
) {
}