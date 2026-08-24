package com.earthy.shop.domain.payment.client;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.payment.dto.portone.PortOneCancelRequestDto;
import com.earthy.shop.domain.payment.dto.portone.PortOnePaymentResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class PortOnePaymentClient {

    private final RestClient restClient;
    private final String apiSecret;

    public PortOnePaymentClient(
            @Value("${portone.api-secret:}") String apiSecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.portone.io")
                .build();
        this.apiSecret = apiSecret.trim();
    }

    // 결제 단건 조회로 승인 결과 검증
    public PortOnePaymentResponseDto getPayment(String paymentId) {
        return requestPayment(paymentId, ErrorCode.PAYMENT_CONFIRM_FAILED, "[PORTONE PAYMENT VERIFY FAILED]");
    }

    // 결제 취소 후 PortOne 원격 상태 재확인
    public PortOnePaymentResponseDto getPaymentForCancelVerification(String paymentId) {
        return requestPayment(paymentId, ErrorCode.PAYMENT_CANCEL_FAILED, "[PORTONE PAYMENT CANCEL VERIFY FAILED]");
    }

    // 결제 취소 요청
    public PortOnePaymentResponseDto cancelPayment(String paymentId, String reason) {
        validateApiSecret(ErrorCode.PAYMENT_CANCEL_FAILED);

        try {
            PortOnePaymentResponseDto response = restClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .header(HttpHeaders.AUTHORIZATION, createAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PortOneCancelRequestDto(reason))
                    .retrieve()
                    .body(PortOnePaymentResponseDto.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
            }

            log.info("[PORTONE PAYMENT CANCEL RESPONSE] paymentId={} | status={} | nestedPaymentStatus={} | cancellationStatus={}",
                    paymentId,
                    response.status(),
                    response.nestedPaymentStatus(),
                    response.cancellationStatus());

            return response;
        } catch (RestClientException e) {
            log.error("[PORTONE PAYMENT CANCEL FAILED] paymentId={} | reason={} | message={}",
                    paymentId,
                    reason,
                    e.getMessage());

            PortOnePaymentResponseDto verifiedPayment = getPaymentForCancelVerification(paymentId);
            if (verifiedPayment.isCanceledPayment()) {
                log.info("[PORTONE PAYMENT ALREADY CANCELED] paymentId={} | status={}",
                        paymentId,
                        verifiedPayment.status());

                return verifiedPayment;
            }

            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    private PortOnePaymentResponseDto requestPayment(String paymentId, ErrorCode errorCode, String logPrefix) {
        validateApiSecret(errorCode);

        try {
            PortOnePaymentResponseDto response = restClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .header(HttpHeaders.AUTHORIZATION, createAuthorizationHeader())
                    .retrieve()
                    .body(PortOnePaymentResponseDto.class);

            if (response == null) {
                throw new BusinessException(errorCode);
            }

            return response;
        } catch (RestClientException e) {
            log.error("{} paymentId={} | message={}",
                    logPrefix,
                    paymentId,
                    e.getMessage());

            throw new BusinessException(errorCode);
        }
    }

    // PortOne V2 API 인증 헤더 생성
    private String createAuthorizationHeader() {
        return "PortOne " + apiSecret;
    }

    // PortOne API Secret 설정 여부 검증
    private void validateApiSecret(ErrorCode errorCode) {
        if (apiSecret.isBlank()) {
            throw new BusinessException(errorCode);
        }
    }
}
