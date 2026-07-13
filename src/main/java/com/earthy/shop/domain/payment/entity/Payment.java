package com.earthy.shop.domain.payment.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 결제 키
    @Column(nullable = false, unique = true)
    private String paymentKey;

    // 결제 주문 번호
    @Column(nullable = false)
    private String orderNumber;

    // 결제 금액
    @Column(nullable = false)
    private int amount;

    // 결제 수단
    private String method;

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    public Payment(
            Order order,
            String paymentKey,
            String orderNumber,
            int amount,
            String method,
            PaymentStatus status
    ) {
        this.order = order;
        this.paymentKey = paymentKey;
        this.orderNumber = orderNumber;
        this.amount = amount;
        this.method = method;
        this.status = status;
    }

    // 결제 취소
    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }
}
