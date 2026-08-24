package com.earthy.shop.domain.notification.service;

import com.earthy.shop.domain.notification.client.AlimtalkClient;
import com.earthy.shop.domain.notification.dto.AlimtalkMessageRequestDto;
import com.earthy.shop.domain.notification.enums.AlimtalkTemplateCode;
import com.earthy.shop.domain.notification.enums.OrderCancelRequester;
import com.earthy.shop.domain.notification.event.OrderCanceledNotificationEvent;
import com.earthy.shop.domain.notification.event.OrderCompletedNotificationEvent;
import com.earthy.shop.domain.notification.event.ShippingStartedNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

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
                Map.of(
                        "#{ordererName}", value(event.ordererName()),
                        "#{orderDate}", value(event.orderDate()),
                        "#{orderNumber}", value(event.orderNumber()),
                        "#{productName}", value(event.productName()),
                        "#{totalPrice}", "%,d".formatted(event.totalPrice())
                ),
                null,
                null,
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
                Map.of(
                        "#{ordererName}", value(event.ordererName()),
                        "#{carrier}", value(event.carrier()),
                        "#{trackingNumber}", value(event.trackingNumber())
                ),
                SHIPPING_TRACKING_BUTTON_NAME,
                event.trackingUrl(),
                event.carrier(),
                event.trackingNumber()
        ));
    }

    // 주문 취소 알림 발송
    public void sendOrderCanceled(OrderCanceledNotificationEvent event) {
        AlimtalkTemplateCode templateCode = event.requester() == OrderCancelRequester.ADMIN
                ? AlimtalkTemplateCode.ORDER_CANCELED_BY_ADMIN
                : AlimtalkTemplateCode.ORDER_CANCELED_BY_CUSTOMER;

        String content = event.requester() == OrderCancelRequester.ADMIN
                ? createAdminCanceledContent(event)
                : createCustomerCanceledContent(event);

        alimtalkClient.send(new AlimtalkMessageRequestDto(
                event.receiverPhone(),
                templateCode,
                content,
                createCanceledVariables(event),
                null,
                null,
                null,
                null
        ));
    }

    // 주문 취소 알림 템플릿 변수
    private Map<String, String> createCanceledVariables(OrderCanceledNotificationEvent event) {
        if (event.requester() == OrderCancelRequester.ADMIN) {
            return Map.of(
                    "#{orderNumber}", value(event.orderNumber()),
                    "#{totalPrice}", "%,d".formatted(event.totalPrice()),
                    "#{cancelReason}", value(event.cancelReason())
            );
        }

        return Map.of(
                "#{orderNumber}", value(event.orderNumber()),
                "#{totalPrice}", "%,d".formatted(event.totalPrice())
        );
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    // 고객 직접 취소 알림 본문
    private String createCustomerCanceledContent(OrderCanceledNotificationEvent event) {
        return """
                [EARTHY] 주문 취소 완료
                주문번호: %s
                주문금액: %,d원

                요청하신 주문 취소가 완료되었습니다.
                결제 취소 금액은 결제수단에 따라 반영까지 시간이 소요될 수 있습니다.
                """.formatted(
                event.orderNumber(),
                event.totalPrice()
        );
    }

    // 관리자 취소 알림 본문
    private String createAdminCanceledContent(OrderCanceledNotificationEvent event) {
        return """
                [EARTHY] 주문 취소 안내
                주문번호: %s
                주문금액: %,d원
                취소사유: %s

                위 사유로 주문이 취소되었습니다.
                결제 취소 금액은 결제수단에 따라 반영까지 시간이 소요될 수 있습니다.
                문의사항은 Q&A 게시판을 이용해 주세요.
                """.formatted(
                event.orderNumber(),
                event.totalPrice(),
                event.cancelReason()
        );
    }
}
