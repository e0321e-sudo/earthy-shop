package com.earthy.shop.domain.notification.client;

import com.earthy.shop.domain.notification.dto.AlimtalkMessageRequestDto;

public interface AlimtalkClient {

    // 알림톡 발송
    void send(AlimtalkMessageRequestDto requestDto);
}
