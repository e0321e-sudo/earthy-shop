package com.earthy.shop.domain.order.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
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

    // 택배사
    private String carrier;

    // 송장번호
    private String trackingNumber;

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
        validateStatusChange(status);

        this.status = status;
    }

    // 주문 상태 변경 검증
    private void validateStatusChange(OrderStatus nextStatus) {
        boolean valid =
                (this.status == OrderStatus.PAID && nextStatus == OrderStatus.PREPARING)
                        || (this.status == OrderStatus.PREPARING && nextStatus == OrderStatus.SHIPPED)
                        || (this.status == OrderStatus.SHIPPED && nextStatus == OrderStatus.DELIVERED);

        if (!valid) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_CHANGE);
        }
    }

    // 주문 결제 완료
    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_CHANGE);
        }

        this.status = OrderStatus.PAID;
    }

    // 주문 상품 추가
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    // 주문 취소
    public void cancel() {
        if (this.status != OrderStatus.PENDING
                && this.status != OrderStatus.PAID
                && this.status != OrderStatus.PREPARING) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE);
        }

        this.status = OrderStatus.CANCELED;
    }

    // 배송 정보 등록
    public void registerShipment(String carrier, String trackingNumber) {
        if (carrier == null || carrier.isBlank()
                || trackingNumber == null || trackingNumber.isBlank()) {
            throw new BusinessException(ErrorCode.SHIPPING_INFO_REQUIRED);
        }

        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
    }
}
