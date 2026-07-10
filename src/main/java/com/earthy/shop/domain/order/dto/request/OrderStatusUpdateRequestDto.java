package com.earthy.shop.domain.order.dto.request;

import com.earthy.shop.domain.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequestDto {

    // 주문 상태
    @NotNull(message = "주문 상태를 입력해주세요.")
    private OrderStatus status;
}
