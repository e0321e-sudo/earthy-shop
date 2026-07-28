package com.earthy.shop.domain.notification.service;

import com.earthy.shop.domain.notification.event.OrderCompletedNotificationEvent;
import com.earthy.shop.domain.notification.event.ShippingStartedNotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private KakaoAlimtalkService kakaoAlimtalkService;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    void 주문_완료_이벤트를_받으면_알림톡을_발송한다() {
        // given
        OrderCompletedNotificationEvent event = orderCompletedEvent();

        // when
        notificationEventListener.handleOrderCompleted(event);

        // then
        verify(kakaoAlimtalkService).sendOrderCompleted(event);
    }

    @Test
    void 주문_완료_알림톡_발송에_실패해도_예외를_전파하지_않는다() {
        // given
        OrderCompletedNotificationEvent event = orderCompletedEvent();

        willThrow(new RuntimeException("alimtalk error"))
                .given(kakaoAlimtalkService)
                .sendOrderCompleted(event);

        // when
        notificationEventListener.handleOrderCompleted(event);

        // then
        verify(kakaoAlimtalkService).sendOrderCompleted(event);
    }

    @Test
    void 상품_발송_이벤트를_받으면_알림톡을_발송한다() {
        // given
        ShippingStartedNotificationEvent event = shippingStartedEvent();

        // when
        notificationEventListener.handleShippingStarted(event);

        // then
        verify(kakaoAlimtalkService).sendShippingStarted(event);
    }

    @Test
    void 상품_발송_알림톡_발송에_실패해도_예외를_전파하지_않는다() {
        // given
        ShippingStartedNotificationEvent event = shippingStartedEvent();

        willThrow(new RuntimeException("alimtalk error"))
                .given(kakaoAlimtalkService)
                .sendShippingStarted(event);

        // when
        notificationEventListener.handleShippingStarted(event);

        // then
        verify(kakaoAlimtalkService).sendShippingStarted(event);
    }

    private OrderCompletedNotificationEvent orderCompletedEvent() {
        return new OrderCompletedNotificationEvent(
                "010-1234-5678",
                "박수지",
                "2026-07-28",
                "ORD-20260728-ABC12345",
                "sunset sea postcard",
                6000
        );
    }

    private ShippingStartedNotificationEvent shippingStartedEvent() {
        return new ShippingStartedNotificationEvent(
                "010-1234-5678",
                "박수지",
                "우체국",
                "6082031227559",
                "https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?sid1=6082031227559"
        );
    }
}
