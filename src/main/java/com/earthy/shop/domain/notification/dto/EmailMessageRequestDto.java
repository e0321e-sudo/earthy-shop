package com.earthy.shop.domain.notification.dto;

// 이메일 발송 요청 DTO
public record EmailMessageRequestDto(
        String receiverEmail,
        String subject,
        String content
) {
}
