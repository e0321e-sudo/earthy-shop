package com.earthy.shop.domain.notification.client;

import com.earthy.shop.domain.notification.dto.AlimtalkMessageRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "solapi", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingAlimtalkClient implements AlimtalkClient {

    // 알림톡 발송 로그
    @Override
    public void send(AlimtalkMessageRequestDto requestDto) {
        log.info(
                "[ALIMTALK READY] receiver={} | template={} | variables={} | buttonName={} | buttonUrl={} | carrier={} | trackingNumber={} | content={}",
                requestDto.receiverPhone(),
                requestDto.templateCode(),
                requestDto.variables(),
                requestDto.buttonName(),
                requestDto.buttonUrl(),
                requestDto.carrier(),
                requestDto.trackingNumber(),
                requestDto.content()
        );
    }
}
