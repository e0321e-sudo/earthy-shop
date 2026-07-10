package com.earthy.shop.domain.order.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문 상품
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 주문번호
    @Column(nullable = false, unique = true)
    private String orderNumber;

    // 주문 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 수령인
    @Column(nullable = false)
    private String receiverName;
    // 수령인 연락처
    @Column(nullable = false)
    private String receiverPhone;

    // 우편번호
    @Column(nullable = false)
    private String zipCode;

    // 기본 주소
    @Column(nullable = false)
    private String address;

    // 상세 주소
    @Column(nullable = false)
    private String detailAddress;

    // 배송 메모
    private String deliveryMemo;

    // 주문 총 금액
    @Column(nullable = false)
    private int totalPrice;

    // 주문 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    public Order(
            String orderNumber,
            Member member,
            String receiverName,
            String receiverPhone,
            String zipCode,
            String address,
            String detailAddress,
            String deliveryMemo,
            int totalPrice
    ) {
        this.orderNumber = orderNumber;
        this.member = member;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.detailAddress = detailAddress;
        this.deliveryMemo = deliveryMemo;
        this.totalPrice = totalPrice;
    }

    // 주문 상태 변경
    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    // 주문 상품 추가
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }
}
