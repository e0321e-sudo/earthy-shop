package com.earthy.shop.domain.notification.event;

import com.earthy.shop.domain.notification.enums.OrderCancelRequester;

// 주문 취소 알림 이벤트
public record OrderCanceledNotificationEvent(
        String receiverPhone,
        String orderNumber,
        int totalPrice,
        String cancelReason,
        OrderCancelRequester requester
) {
}
