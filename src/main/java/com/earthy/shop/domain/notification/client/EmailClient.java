package com.earthy.shop.domain.notification.client;

import com.earthy.shop.domain.notification.dto.EmailMessageRequestDto;

public interface EmailClient {

    // 이메일 발송
    void send(EmailMessageRequestDto requestDto);
}
