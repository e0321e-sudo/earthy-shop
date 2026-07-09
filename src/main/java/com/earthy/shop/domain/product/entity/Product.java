package com.earthy.shop.domain.product.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.domain.product.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품명
    @Column(nullable = false)
    private String name;

    // 상품 카테고리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    // 상품 가격
    @Column(nullable = false)
    private int price;

    // 상품 이미지 경로
    @Column(nullable = false)
    private String imageUrl;

    // 상품 설명
    @Column(columnDefinition = "TEXT")
    private String description;

    // 재고 수량
    @Column(nullable = false)
    private int stockQuantity;

    // 판매 활성 상태
    @Column(nullable = false)
    private boolean active = true;

    public Product(
            String name,
            ProductCategory category,
            int price,
            String imageUrl,
            String description,
            int stockQuantity
    ) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.description = description;
        this.stockQuantity = stockQuantity;
        this.active = true;
    }

    // 상품 수정
    public void update(
            String name,
            ProductCategory category,
            int price,
            String imageUrl,
            String description,
            int stockQuantity
    ) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.description = description;
        this.stockQuantity = stockQuantity;
    }

    // 상품 비활성화
    public void deactivate() {
        this.active = false;
    }
}
