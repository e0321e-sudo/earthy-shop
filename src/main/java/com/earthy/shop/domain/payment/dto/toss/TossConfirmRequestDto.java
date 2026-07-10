package com.earthy.shop.domain.payment.dto.toss;

public record TossConfirmRequestDto(
        String paymentKey,
        String orderId,
        int amount
) {
}