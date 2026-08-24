package com.earthy.shop.domain.notification.client;

import com.earthy.shop.domain.notification.dto.AlimtalkMessageRequestDto;
import com.earthy.shop.domain.notification.enums.AlimtalkTemplateCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "solapi", name = "enabled", havingValue = "true")
public class SolapiAlimtalkClient implements AlimtalkClient {

    private static final String SOLAPI_SEND_MANY_URL = "https://api.solapi.com/messages/v4/send-many/detail";
    private static final String SOLAPI_AUTH_SCHEME = "HMAC-SHA256";
    private static final String SHIPPING_TRACKING_BUTTON_TYPE = "DS";
    private static final String KAKAO_OPTION_PF_ID = "pfId";
    private static final String KAKAO_OPTION_TEMPLATE_ID = "templateId";
    private static final String KAKAO_OPTION_VARIABLES = "variables";
    private static final String KAKAO_OPTION_BUTTONS = "buttons";

    private final RestClient restClient = RestClient.create();

    @Value("${solapi.api-key:}")
    private String apiKey;

    @Value("${solapi.api-secret:}")
    private String apiSecret;

    @Value("${solapi.pf-id:}")
    private String pfId;

    @Value("${solapi.sender-phone:}")
    private String senderPhone;

    @Value("${solapi.template.order-completed:}")
    private String orderCompletedTemplateId;

    @Value("${solapi.template.shipping-started:}")
    private String shippingStartedTemplateId;

    @Value("${solapi.template.canceled-customer:}")
    private String canceledCustomerTemplateId;

    @Value("${solapi.template.canceled-admin:}")
    private String canceledAdminTemplateId;

    // 솔라피 알림톡 발송
    @Override
    public void send(AlimtalkMessageRequestDto requestDto) {
        String templateId = resolveTemplateId(requestDto.templateCode());

        if (isBlank(apiKey) || isBlank(apiSecret) || isBlank(pfId)
                || isBlank(senderPhone) || isBlank(templateId)) {
            log.warn(
                    "[SOLAPI ALIMTALK SKIPPED] template={} | reason=missing solapi config",
                    requestDto.templateCode()
            );
            return;
        }

        Map<String, Object> kakaoOptions = new LinkedHashMap<>();
        kakaoOptions.put(KAKAO_OPTION_PF_ID, pfId);
        kakaoOptions.put(KAKAO_OPTION_TEMPLATE_ID, templateId);
        kakaoOptions.put(KAKAO_OPTION_VARIABLES, requestDto.variables() == null ? Map.of() : requestDto.variables());

        List<Map<String, String>> buttons = createButtons(requestDto);
        if (!buttons.isEmpty()) {
            kakaoOptions.put(KAKAO_OPTION_BUTTONS, buttons);
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("to", normalizePhone(requestDto.receiverPhone()));
        message.put("from", normalizePhone(senderPhone));
        message.put("kakaoOptions", kakaoOptions);

        Map<String, Object> body = Map.of("messages", List.of(message));

        try {
            restClient.post()
                    .uri(SOLAPI_SEND_MANY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", createAuthorizationHeader())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "[SOLAPI ALIMTALK SENT] receiver={} | template={} | templateId={}",
                    requestDto.receiverPhone(),
                    requestDto.templateCode(),
                    templateId
            );
        } catch (RestClientResponseException e) {
            log.error(
                    "[SOLAPI ALIMTALK FAILED] receiver={} | template={} | status={} | response={}",
                    requestDto.receiverPhone(),
                    requestDto.templateCode(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw e;
        } catch (RestClientException e) {
            log.error(
                    "[SOLAPI ALIMTALK FAILED] receiver={} | template={} | message={}",
                    requestDto.receiverPhone(),
                    requestDto.templateCode(),
                    e.getMessage()
            );
            throw e;
        }
    }

    // 템플릿 코드별 솔라피 템플릿 ID 매핑
    private String resolveTemplateId(AlimtalkTemplateCode templateCode) {
        return switch (templateCode) {
            case ORDER_COMPLETED -> orderCompletedTemplateId;
            case SHIPPING_STARTED -> shippingStartedTemplateId;
            case ORDER_CANCELED_BY_CUSTOMER -> canceledCustomerTemplateId;
            case ORDER_CANCELED_BY_ADMIN -> canceledAdminTemplateId;
        };
    }

    // 배송시작 알림은 알림톡 배송조회 버튼 타입을 사용
    private List<Map<String, String>> createButtons(AlimtalkMessageRequestDto requestDto) {
        if (requestDto.templateCode() != AlimtalkTemplateCode.SHIPPING_STARTED
                || isBlank(requestDto.buttonName())) {
            return List.of();
        }

        List<Map<String, String>> buttons = new ArrayList<>();
        Map<String, String> shippingTrackingButton = new LinkedHashMap<>();
        shippingTrackingButton.put("buttonType", SHIPPING_TRACKING_BUTTON_TYPE);
        shippingTrackingButton.put("buttonName", requestDto.buttonName());
        buttons.add(shippingTrackingButton);

        return buttons;
    }

    // 솔라피 API 인증 헤더 생성
    private String createAuthorizationHeader() {
        String date = Instant.now().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        String signature = createHmacSignature(date + salt);

        return "%s apiKey=%s, date=%s, salt=%s, signature=%s"
                .formatted(SOLAPI_AUTH_SCHEME, apiKey, date, salt, signature);
    }

    private String createHmacSignature(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("솔라피 인증 서명 생성에 실패했습니다.", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
