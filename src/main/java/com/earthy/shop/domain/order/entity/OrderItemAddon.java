package com.earthy.shop.domain.order.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 주문 추가상품 스냅샷 엔티티
@Getter
@Entity
@Table(name = "order_item_addons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemAddon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // 추가상품 ID 스냅샷
    @Column(nullable = false)
    private Long addonId;

    // 추가상품명 스냅샷
    @Column(nullable = false)
    private String addonName;

    // 추가상품 가격 스냅샷
    @Column(nullable = false)
    private int addonPrice;

    // 추가상품 수량 스냅샷
    @Column(nullable = false)
    private int quantity;

    public OrderItemAddon(Long addonId, String addonName, int addonPrice, int quantity) {
        this.addonId = addonId;
        this.addonName = addonName;
        this.addonPrice = addonPrice;
        this.quantity = quantity;
    }

    // 주문 상품 지정
    public void assignOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    // 추가상품 총 금액
    public int calculateTotalPrice() {
        return addonPrice * quantity;
    }
}
