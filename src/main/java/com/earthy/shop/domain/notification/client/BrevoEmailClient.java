package com.earthy.shop.domain.notification.client;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.notification.dto.EmailMessageRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class BrevoEmailClient implements EmailClient {

    private static final String BREVO_SEND_EMAIL_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient = RestClient.create();

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.sender-name:EARTHY}")
    private String senderName;

    @Value("${brevo.sender-email:}")
    private String senderEmail;

    // Brevo 이메일 발송
    @Override
    public void send(EmailMessageRequestDto requestDto) {
        validateBrevoConfig();

        BrevoEmailRequest brevoRequest = new BrevoEmailRequest(
                new BrevoSender(senderName, senderEmail),
                List.of(new BrevoReceiver(requestDto.receiverEmail())),
                requestDto.subject(),
                requestDto.content()
        );

        try {
            restClient.post()
                    .uri(BREVO_SEND_EMAIL_URL)
                    .header("api-key", apiKey)
                    .body(brevoRequest)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "[EMAIL SENT] receiver={} | subject={}",
                    requestDto.receiverEmail(),
                    requestDto.subject()
            );
        } catch (RestClientResponseException e) {
            log.error(
                    "[EMAIL SEND FAILED] receiver={} | subject={} | status={} | response={}",
                    requestDto.receiverEmail(),
                    requestDto.subject(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        } catch (RestClientException e) {
            log.error(
                    "[EMAIL SEND FAILED] receiver={} | subject={} | message={}",
                    requestDto.receiverEmail(),
                    requestDto.subject(),
                    e.getMessage()
            );
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    // Brevo 설정값 검증
    private void validateBrevoConfig() {
        if (apiKey == null || apiKey.isBlank()
                || senderEmail == null || senderEmail.isBlank()) {
            throw new BusinessException(ErrorCode.EMAIL_CONFIG_NOT_FOUND);
        }
    }

    private record BrevoEmailRequest(
            BrevoSender sender,
            List<BrevoReceiver> to,
            String subject,
            String textContent
    ) {
    }

    private record BrevoSender(
            String name,
            String email
    ) {
    }

    private record BrevoReceiver(
            String email
    ) {
    }
}
