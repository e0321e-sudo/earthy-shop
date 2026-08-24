package com.earthy.shop.domain.notification.dto;

import com.earthy.shop.domain.notification.enums.AlimtalkTemplateCode;

import java.util.Map;

// 알림톡 발송 요청 DTO
public record AlimtalkMessageRequestDto(
        String receiverPhone,
        AlimtalkTemplateCode templateCode,
        String content,
        Map<String, String> variables,
        String buttonName,
        String buttonUrl,
        String carrier,
        String trackingNumber
) {
}
