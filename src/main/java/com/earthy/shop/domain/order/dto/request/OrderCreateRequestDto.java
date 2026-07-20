package com.earthy.shop.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

    // 주문할 장바구니 상품 ID 목록
    // null 또는 빈 목록이면 전체상품주문
    private List<Long> cartItemIds;

    @NotBlank(message = "수령인을 입력해주세요.")
    private String receiverName;

    @NotBlank(message = "수령인 연락처를 입력해주세요.")
    @Pattern(
            regexp = "^01[016789]-\\d{4}-\\d{4}$",
            message = "전화번호 형식은 01X-0000-0000 이어야 합니다."
    )
    private String receiverPhone;

    @NotBlank(message = "우편번호를 입력해주세요.")
    private String zipCode;

    @NotBlank(message = "기본 주소를 입력해주세요.")
    private String address;

    private String detailAddress;

    private String deliveryMemo;
}
