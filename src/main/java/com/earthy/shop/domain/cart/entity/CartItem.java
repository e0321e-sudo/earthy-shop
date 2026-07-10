package com.earthy.shop.domain.cart.entity;

import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장바구니 상품 엔티티
@Getter
@Entity
@Table(name = "cart_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 추가상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_id")
    private Addon addon;

    // 수량
    @Column(nullable = false)
    private int quantity;

    // 추가상품 수량
    @Column(nullable = false)
    private int addonQuantity;

    public CartItem(
            Member member,
            Product product,
            Addon addon,
            int quantity,
            int addonQuantity
    ) {
        this.member = member;
        this.product = product;
        this.addon = addon;
        this.quantity = quantity;
        this.addonQuantity = addonQuantity;
    }

    // 수량 변경
    public void updateQuantity(int quantity, int addonQuantity) {
        this.quantity = quantity;
        this.addonQuantity = addonQuantity;
    }

    // 수량 증가
    public void increaseQuantity(int quantity, int addonQuantity) {
        this.quantity += quantity;
        this.addonQuantity += addonQuantity;
    }
}
