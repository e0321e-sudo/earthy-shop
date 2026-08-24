package com.earthy.shop.domain.notification.service;

import com.earthy.shop.domain.notification.event.OrderCompletedNotificationEvent;
import com.earthy.shop.domain.notification.event.OrderCanceledNotificationEvent;
import com.earthy.shop.domain.notification.event.ShippingStartedNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final KakaoAlimtalkService kakaoAlimtalkService;

    // 주문 완료 알림 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedNotificationEvent event) {
        try {
            kakaoAlimtalkService.sendOrderCompleted(event);
        } catch (Exception e) {
            log.error("[ORDER COMPLETED ALIMTALK FAILED] orderNumber={}", event.orderNumber(), e);
        }
    }

    // 상품 발송 알림 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleShippingStarted(ShippingStartedNotificationEvent event) {
        try {
            kakaoAlimtalkService.sendShippingStarted(event);
        } catch (Exception e) {
            log.error("[SHIPPING STARTED ALIMTALK FAILED] trackingNumber={}", event.trackingNumber(), e);
        }
    }

    // 주문 취소 알림 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCanceled(OrderCanceledNotificationEvent event) {
        try {
            kakaoAlimtalkService.sendOrderCanceled(event);
        } catch (Exception e) {
            log.error("[ORDER CANCELED ALIMTALK FAILED] orderNumber={} | requester={}",
                    event.orderNumber(),
                    event.requester(),
                    e);
        }
    }
}
