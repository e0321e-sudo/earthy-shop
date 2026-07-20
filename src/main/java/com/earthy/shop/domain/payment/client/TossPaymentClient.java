package com.earthy.shop.domain.payment.client;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.payment.dto.toss.TossCancelRequestDto;
import com.earthy.shop.domain.payment.dto.toss.TossConfirmRequestDto;
import com.earthy.shop.domain.payment.dto.toss.TossConfirmResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class TossPaymentClient {

    private final RestClient restClient;
    private final String secretKey;

    public TossPaymentClient(
            @Value("${toss.secret-key}") String secretKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.tosspayments.com")
                .build();
        this.secretKey = secretKey;
    }

    // 결제 승인 요청
    public TossConfirmResponseDto confirmPayment(TossConfirmRequestDto requestDto) {
        try {
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, createAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestDto)
                    .retrieve()
                    .body(TossConfirmResponseDto.class);
        } catch (RestClientException e) {
            log.error("[TOSS PAYMENT CONFIRM FAILED] orderId={} | amount={} | message={}",
                    requestDto.orderId(),
                    requestDto.amount(),
                    e.getMessage());

            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    // 결제 취소 요청
    public TossConfirmResponseDto cancelPayment(String paymentKey, TossCancelRequestDto requestDto) {
        try {
            return restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, createAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestDto)
                    .retrieve()
                    .body(TossConfirmResponseDto.class);
        } catch (RestClientException e) {
            log.error("[TOSS PAYMENT CANCEL FAILED] paymentKey={} | reason={} | message={}",
                    paymentKey,
                    requestDto.cancelReason(),
                    e.getMessage());

            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    // 인증 헤더 생성
    private String createAuthorizationHeader() {
        String credentials = secretKey + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return "Basic " + encodedCredentials;
    }
}
