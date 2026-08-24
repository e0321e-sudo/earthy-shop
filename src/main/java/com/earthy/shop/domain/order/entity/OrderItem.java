package com.earthy.shop.domain.order.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name ="order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 상품 ID 스냅샷
    @Column(nullable = false)
    private Long productId;

    // 상품명 스냅샷
    @Column(nullable = false)
    private String productName;

    // 상품 이미지 경로 스냅샷
    @Column(nullable = false)
    private String productImageUrl;

    // 상품 가격 스냅샷
    @Column(nullable = false)
    private int productPrice;

    // 포스터 사이즈 옵션 ID 스냅샷
    private Long sizeOptionId;

    // 선택한 사이즈명 스냅샷
    private String sizeName;

    // 선택 당시 사이즈 추가금
    @Column(nullable = false)
    private int sizeAdditionalPrice;

    // 상품 기본가 + 사이즈 추가금
    @Column(nullable = false)
    private int productUnitPrice;

    // 추가상품 ID 스냅샷
    private Long addonId;

    // 추가상품명 스냅샷
    private String addonName;

    // 추가 상품 가격 스냅샷
    @Column(nullable = false)
    private int addonPrice;

    // 추가상품 수량 스냅샷
    @Column(nullable = false)
    private int addonQuantity;

    // 주문 수량
    @Column(nullable = false)
    private int quantity;

    // 주문 상품 금액
    @Column(nullable = false)
    private int itemTotalPrice;

    // 복수 추가상품 스냅샷
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemAddon> orderItemAddons = new ArrayList<>();

    public OrderItem(
            Long productId,
            String productName,
            String productImageUrl,
            int productPrice,
            Long addonId,
            String addonName,
            int addonPrice,
            int addonQuantity,
            int quantity
    ) {
        this(
                productId,
                productName,
                productImageUrl,
                productPrice,
                null,
                null,
                0,
                productPrice,
                addonId,
                addonName,
                addonPrice,
                addonQuantity,
                quantity
        );
    }

    public OrderItem(
            Long productId,
            String productName,
            String productImageUrl,
            int productPrice,
            Long sizeOptionId,
            String sizeName,
            int sizeAdditionalPrice,
            int productUnitPrice,
            Long addonId,
            String addonName,
            int addonPrice,
            int addonQuantity,
            int quantity
    ) {
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.productPrice = productPrice;
        this.sizeOptionId = sizeOptionId;
        this.sizeName = sizeName;
        this.sizeAdditionalPrice = sizeAdditionalPrice;
        this.productUnitPrice = productUnitPrice;
        this.addonId = addonId;
        this.addonName = addonName;
        this.addonPrice = addonPrice;
        this.addonQuantity = addonQuantity;
        this.quantity = quantity;
        this.itemTotalPrice = calculateItemTotalPrice();
    }

    // 주문 지정
    public void assignOrder(Order order) {
        this.order = order;
    }

    // 복수 추가상품 추가
    public void addOrderItemAddon(OrderItemAddon orderItemAddon) {
        orderItemAddons.add(orderItemAddon);
        orderItemAddon.assignOrderItem(this);
        this.itemTotalPrice = calculateItemTotalPrice();
    }

    // 주문 상품 총 금액 계산
    private int calculateItemTotalPrice() {
        int addonTotalPrice = orderItemAddons.isEmpty()
                ? addonPrice * addonQuantity
                : orderItemAddons.stream()
                .mapToInt(OrderItemAddon::calculateTotalPrice)
                .sum();

        return (productUnitPrice * quantity) + addonTotalPrice;
    }
}
