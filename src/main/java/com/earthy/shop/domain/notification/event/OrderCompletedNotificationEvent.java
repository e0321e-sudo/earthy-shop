package com.earthy.shop.domain.notification.event;

// 주문 완료 알림 이벤트
public record OrderCompletedNotificationEvent(
        String receiverPhone,
        String ordererName,
        String orderDate,
        String orderNumber,
        String productName,
        int totalPrice
) {
}
