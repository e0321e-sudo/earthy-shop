package com.earthy.shop.domain.notification.service;

import com.earthy.shop.domain.notification.client.AlimtalkClient;
import com.earthy.shop.domain.notification.dto.AlimtalkMessageRequestDto;
import com.earthy.shop.domain.notification.enums.AlimtalkTemplateCode;
import com.earthy.shop.domain.notification.event.OrderCompletedNotificationEvent;
import com.earthy.shop.domain.notification.event.ShippingStartedNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoAlimtalkService {

    private static final String SHIPPING_TRACKING_BUTTON_NAME = "배송조회";

    private final AlimtalkClient alimtalkClient;

    // 주문 완료 알림 발송
    public void sendOrderCompleted(OrderCompletedNotificationEvent event) {
        String content = """
                [EARTHY] 주문 완료

                %s님

                주문일: %s
                주문번호: %s
                상품명: %s
                주문금액: %,d원

                주문해주셔서 감사합니다.
                당신의 일상에 작은 장면이 되어드릴 수 있도록
                정성껏 준비해 보내드리겠습니다.
                """.formatted(
                event.ordererName(),
                event.orderDate(),
                event.orderNumber(),
                event.productName(),
                event.totalPrice()
        );

        alimtalkClient.send(new AlimtalkMessageRequestDto(
                event.receiverPhone(),
                AlimtalkTemplateCode.ORDER_COMPLETED,
                content,
                null,
                null
        ));
    }

    // 상품 발송 알림 발송
    public void sendShippingStarted(ShippingStartedNotificationEvent event) {
        String content = """
                [EARTHY] 상품 발송 안내

                %s님

                택배사: %s
                송장번호: %s

                상품이 발송되었습니다.
                """.formatted(
                event.ordererName(),
                event.carrier(),
                event.trackingNumber()
        );

        alimtalkClient.send(new AlimtalkMessageRequestDto(
                event.receiverPhone(),
                AlimtalkTemplateCode.SHIPPING_STARTED,
                content,
                SHIPPING_TRACKING_BUTTON_NAME,
                event.trackingUrl()
        ));
    }
}
