package com.earthy.shop.domain.payment.dto.response;

import com.earthy.shop.domain.payment.entity.Payment;
import com.earthy.shop.domain.payment.enums.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponseDto(
            Long paymentId,
            Long orderId,
            String paymentKey,
            String orderNumber,
            int amount,
            String method,
            PaymentStatus status,
            String statusDescription,
            LocalDateTime createdAt
    ) {
        public static PaymentResponseDto from(Payment payment) {
            return new PaymentResponseDto(
                    payment.getId(),
                    payment.getOrder().getId(),
                    payment.getPaymentKey(),
                    payment.getOrderNumber(),
                    payment.getAmount(),
                    payment.getMethod(),
                    payment.getStatus(),
                    payment.getStatus().getDescription(),
                    payment.getCreatedAt()
            );
        }
    }
