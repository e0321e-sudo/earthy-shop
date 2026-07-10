package com.earthy.shop.domain.order.dto.response;

import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
        Long orderId,
        String orderNumber,
        String receiverName,
        String receiverPhone,
        String zipCode,
        String address,
        String detailAddress,
        String deliveryMemo,
        int totalPrice,
        OrderStatus status,
        String statusDescription,
        List<OrderItemResponseDto> items,
        LocalDateTime createdAt
) {
    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getZipCode(),
                order.getAddress(),
                order.getDetailAddress(),
                order.getDeliveryMemo(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getStatus().getDescription(),
                order.getOrderItems()
                        .stream()
                        .map(OrderItemResponseDto::from)
                        .toList(),
                order.getCreatedAt()
        );
    }
}
