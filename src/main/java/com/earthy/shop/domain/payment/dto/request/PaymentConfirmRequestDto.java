package com.earthy.shop.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequestDto {

    // 주문 ID
    @NotNull(message = "주문 ID를 입력해주세요.")
    private Long orderId;

    // 결제 키
    @NotBlank(message = "결제 키를 입력해주세요.")
    private String paymentKey;

    // 결제 금액
    @NotNull(message = "결제 금액을 입력해주세요.")
    private Integer amount;

    // 결제 수단
    private String method;
}
