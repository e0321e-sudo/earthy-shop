package com.earthy.shop.domain.notification.dto;

import com.earthy.shop.domain.notification.enums.AlimtalkTemplateCode;

// 알림톡 발송 요청 DTO
public record AlimtalkMessageRequestDto(
        String receiverPhone,
        AlimtalkTemplateCode templateCode,
        String content,
        String buttonName,
        String buttonUrl
) {
}
