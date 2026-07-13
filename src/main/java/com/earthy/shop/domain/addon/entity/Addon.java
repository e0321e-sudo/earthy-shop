package com.earthy.shop.domain.addon.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.addon.enums.AddonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "addons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Addon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 추가상품명
    @Column(nullable = false)
    private String name;

    // 추가상품 종류
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddonType type;

    // 추가상품 가격
    @Column(nullable = false)
    private int price;

    // 재고 수량
    @Column(nullable = false)
    private int stockQuantity;

    // 판매 활성 상태
    @Column(nullable = false)
    private boolean active = true;

    // 추가상품 생성
    public Addon(
            String name,
            AddonType type,
            int price,
            int stockQuantity
    ) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.active = true;
    }

    // 추가상품 수정
    public void update(
            String name,
            AddonType type,
            int price,
            int stockQuantity
    ) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    // 추가상품 비활성화
    public void deactivate() {
        this.active = false;
    }

    // 재고 차감
    public void decreaseStock(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        if (this.stockQuantity < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }

        this.stockQuantity -= quantity;
    }

    // 재고 증가
    public void increaseStock(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        this.stockQuantity += quantity;
    }
}
