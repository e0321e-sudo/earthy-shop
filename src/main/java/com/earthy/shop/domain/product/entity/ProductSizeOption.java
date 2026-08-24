package com.earthy.shop.domain.product.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_size_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSizeOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사이즈 옵션이 속한 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 사이즈명
    @Column(nullable = false)
    private String sizeName;

    // 기본 상품 가격에 더해지는 추가 금액
    @Column(nullable = false)
    private int additionalPrice;

    // 사이즈별 재고 수량
    @Column(nullable = false)
    private int stockQuantity;

    // 고객에게 노출할 사이즈인지 여부
    @Column(nullable = false)
    private boolean active;

    // 삭제 상태
    @Column(nullable = false)
    private boolean deleted = false;

    public ProductSizeOption(
            Product product,
            String sizeName,
            int additionalPrice,
            int stockQuantity,
            boolean active
    ) {
        this.product = product;
        this.sizeName = sizeName;
        this.additionalPrice = additionalPrice;
        this.stockQuantity = stockQuantity;
        this.active = active;
        this.deleted = false;
    }

    // 사이즈 옵션 수정
    public void update(
            String sizeName,
            int additionalPrice,
            int stockQuantity,
            boolean active
    ) {
        this.sizeName = sizeName;
        this.additionalPrice = additionalPrice;
        this.stockQuantity = stockQuantity;
        this.active = active;
    }

    // 사이즈 옵션 삭제
    public void delete() {
        this.deleted = true;
        this.active = false;
    }

    // 사이즈 옵션 재고 차감
    public void decreaseStock(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        if (this.stockQuantity < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }

        this.stockQuantity -= quantity;
    }

    // 사이즈 옵션 재고 증가
    public void increaseStock(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        this.stockQuantity += quantity;
    }
}
