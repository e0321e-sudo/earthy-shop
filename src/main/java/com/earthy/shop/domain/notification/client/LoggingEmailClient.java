package com.earthy.shop.domain.notification.client;

import com.earthy.shop.domain.notification.dto.EmailMessageRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEmailClient implements EmailClient {

    // 이메일 발송 로그
    @Override
    public void send(EmailMessageRequestDto requestDto) {
        log.info(
                "[EMAIL READY] receiver={} | subject={} | content={}",
                requestDto.receiverEmail(),
                requestDto.subject(),
                requestDto.content()
        );
    }
}
