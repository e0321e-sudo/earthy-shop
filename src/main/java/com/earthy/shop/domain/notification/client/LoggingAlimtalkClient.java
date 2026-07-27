package com.earthy.shop.domain.notification.client;

import com.earthy.shop.domain.notification.dto.AlimtalkMessageRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingAlimtalkClient implements AlimtalkClient {

    // 알림톡 발송 로그
    @Override
    public void send(AlimtalkMessageRequestDto requestDto) {
        log.info(
                "[ALIMTALK READY] receiver={} | template={} | buttonName={} | buttonUrl={} | content={}",
                requestDto.receiverPhone(),
                requestDto.templateCode(),
                requestDto.buttonName(),
                requestDto.buttonUrl(),
                requestDto.content()
        );
    }
}
