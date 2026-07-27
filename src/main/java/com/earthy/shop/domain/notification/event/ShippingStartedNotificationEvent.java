package com.earthy.shop.domain.notification.event;

// 상품 발송 알림 이벤트
public record ShippingStartedNotificationEvent(
        String receiverPhone,
        String ordererName,
        String carrier,
        String trackingNumber,
        String trackingUrl
) {
}
