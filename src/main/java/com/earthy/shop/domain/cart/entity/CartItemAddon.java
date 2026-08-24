package com.earthy.shop.domain.cart.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.domain.addon.entity.Addon;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장바구니 추가상품 엔티티
@Getter
@Entity
@Table(name = "cart_item_addons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItemAddon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 장바구니 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    // 추가상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_id", nullable = false)
    private Addon addon;

    // 선택 당시 추가상품명 스냅샷
    @Column(nullable = false)
    private String addonName;

    // 선택 당시 추가상품 가격 스냅샷
    @Column(nullable = false)
    private int addonPrice;

    // 추가상품 수량
    @Column(nullable = false)
    private int quantity;

    public CartItemAddon(CartItem cartItem, Addon addon, int quantity) {
        this.cartItem = cartItem;
        this.addon = addon;
        this.addonName = addon.getName();
        this.addonPrice = addon.getPrice();
        this.quantity = quantity;
    }

    // 장바구니 추가상품 총 금액
    public int calculateTotalPrice() {
        return addonPrice * quantity;
    }

    // 추가상품 수량 변경
    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }

    // 추가상품 수량 증가
    public void increaseQuantity(int quantity) {
        this.quantity += quantity;
    }
}
