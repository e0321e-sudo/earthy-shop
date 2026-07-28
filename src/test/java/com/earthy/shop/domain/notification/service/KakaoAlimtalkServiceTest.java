package com.earthy.shop.domain.notification.service;

import com.earthy.shop.domain.notification.client.AlimtalkClient;
import com.earthy.shop.domain.notification.dto.AlimtalkMessageRequestDto;
import com.earthy.shop.domain.notification.enums.AlimtalkTemplateCode;
import com.earthy.shop.domain.notification.event.OrderCompletedNotificationEvent;
import com.earthy.shop.domain.notification.event.ShippingStartedNotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoAlimtalkServiceTest {

    @Mock
    private AlimtalkClient alimtalkClient;

    @InjectMocks
    private KakaoAlimtalkService kakaoAlimtalkService;

    @Test
    void 주문_완료_알림톡을_발송한다() {
        // given
        OrderCompletedNotificationEvent event = new OrderCompletedNotificationEvent(
                "010-1234-5678",
                "박수지",
                "2026-07-28",
                "ORD-20260728-ABC12345",
                "sunset sea postcard",
                6000
        );
        ArgumentCaptor<AlimtalkMessageRequestDto> captor =
                ArgumentCaptor.forClass(AlimtalkMessageRequestDto.class);

        // when
        kakaoAlimtalkService.sendOrderCompleted(event);

        // then
        verify(alimtalkClient).send(captor.capture());

        AlimtalkMessageRequestDto requestDto = captor.getValue();
        assertThat(requestDto.receiverPhone()).isEqualTo("010-1234-5678");
        assertThat(requestDto.templateCode()).isEqualTo(AlimtalkTemplateCode.ORDER_COMPLETED);
        assertThat(requestDto.content()).contains("[EARTHY] 주문 완료");
        assertThat(requestDto.content()).contains("박수지님");
        assertThat(requestDto.content()).contains("주문일: 2026-07-28");
        assertThat(requestDto.content()).contains("주문번호: ORD-20260728-ABC12345");
        assertThat(requestDto.content()).contains("상품명: sunset sea postcard");
        assertThat(requestDto.content()).contains("주문금액: 6,000원");
        assertThat(requestDto.buttonName()).isNull();
        assertThat(requestDto.buttonUrl()).isNull();
    }

    @Test
    void 상품_발송_알림톡을_발송한다() {
        // given
        ShippingStartedNotificationEvent event = new ShippingStartedNotificationEvent(
                "010-1234-5678",
                "박수지",
                "우체국",
                "6082031227559",
                "https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?sid1=6082031227559"
        );
        ArgumentCaptor<AlimtalkMessageRequestDto> captor =
                ArgumentCaptor.forClass(AlimtalkMessageRequestDto.class);

        // when
        kakaoAlimtalkService.sendShippingStarted(event);

        // then
        verify(alimtalkClient).send(captor.capture());

        AlimtalkMessageRequestDto requestDto = captor.getValue();
        assertThat(requestDto.receiverPhone()).isEqualTo("010-1234-5678");
        assertThat(requestDto.templateCode()).isEqualTo(AlimtalkTemplateCode.SHIPPING_STARTED);
        assertThat(requestDto.content()).contains("[EARTHY] 상품 발송 안내");
        assertThat(requestDto.content()).contains("박수지님");
        assertThat(requestDto.content()).contains("택배사: 우체국");
        assertThat(requestDto.content()).contains("송장번호: 6082031227559");
        assertThat(requestDto.buttonName()).isEqualTo("배송조회");
        assertThat(requestDto.buttonUrl()).isEqualTo(
                "https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?sid1=6082031227559"
        );
    }
}
