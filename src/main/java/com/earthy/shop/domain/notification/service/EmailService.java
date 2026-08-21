package com.earthy.shop.domain.notification.service;

import com.earthy.shop.domain.notification.client.EmailClient;
import com.earthy.shop.domain.notification.dto.EmailMessageRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String TEMP_PASSWORD_SUBJECT = "[EARTHY] 임시비밀번호 안내";

    private final EmailClient emailClient;

    // 임시비밀번호 이메일 발송
    public void sendTemporaryPassword(String receiverEmail, String temporaryPassword) {
        String content = """
                [EARTHY] 임시비밀번호 안내

                요청하신 임시비밀번호를 안내드립니다.

                임시비밀번호: %s

                로그인 후 마이페이지에서 반드시 비밀번호를 변경해주세요.
                """.formatted(temporaryPassword);

        emailClient.send(new EmailMessageRequestDto(
                receiverEmail,
                TEMP_PASSWORD_SUBJECT,
                content
        ));
    }
}
