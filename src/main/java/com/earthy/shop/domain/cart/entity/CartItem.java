package com.earthy.shop.domain.cart.entity;

import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.entity.ProductSizeOption;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    // 포스터 사이즈 옵션
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_option_id")
    private ProductSizeOption productSizeOption;

    // 선택한 사이즈명 스냅샷
    private String selectedSizeName;

    // 선택 당시 사이즈 추가금
    @Column(nullable = false)
    private int sizeAdditionalPrice;

    // 상품 기본가 + 사이즈 추가금
    @Column(nullable = false)
    private int productUnitPrice;

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

    // 복수 추가상품
    @OneToMany(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemAddon> cartItemAddons = new ArrayList<>();

    public CartItem(
            Member member,
            Product product,
            Addon addon,
            int quantity,
            int addonQuantity
    ) {
        this(member, product, null, addon, quantity, addonQuantity);
    }

    public CartItem(
            Member member,
            Product product,
            ProductSizeOption productSizeOption,
            Addon addon,
            int quantity,
            int addonQuantity
    ) {
        this.member = member;
        this.product = product;
        this.productSizeOption = productSizeOption;
        this.selectedSizeName = productSizeOption == null ? null : productSizeOption.getSizeName();
        this.sizeAdditionalPrice = productSizeOption == null ? 0 : productSizeOption.getAdditionalPrice();
        this.productUnitPrice = product.getPrice() + this.sizeAdditionalPrice;
        this.addon = addon;
        this.quantity = quantity;
        this.addonQuantity = addonQuantity;
    }

    // 저장된 단가가 없는 기존 장바구니 데이터는 현재 상품가 기준으로 보정
    public int getEffectiveProductUnitPrice() {
        if (productUnitPrice > 0) {
            return productUnitPrice;
        }

        return product.getPrice() + sizeAdditionalPrice;
    }

    // 장바구니 항목 총 금액
    public int calculateItemTotalPrice() {
        int addonTotalPrice = calculateAddonTotalPrice();

        return (getEffectiveProductUnitPrice() * quantity) + addonTotalPrice;
    }

    // 장바구니 추가상품 총 금액
    private int calculateAddonTotalPrice() {
        if (!cartItemAddons.isEmpty()) {
            return cartItemAddons.stream()
                    .mapToInt(CartItemAddon::calculateTotalPrice)
                    .sum();
        }

        int addonPrice = addon == null ? 0 : addon.getPrice();

        return addonPrice * addonQuantity;
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

    // 복수 추가상품 추가 또는 수량 증가
    public void addOrIncreaseAddon(Addon addon, int quantity) {
        cartItemAddons.stream()
                .filter(cartItemAddon -> Objects.equals(cartItemAddon.getAddon().getId(), addon.getId()))
                .findFirst()
                .ifPresentOrElse(
                        cartItemAddon -> cartItemAddon.increaseQuantity(quantity),
                        () -> cartItemAddons.add(new CartItemAddon(this, addon, quantity))
                );
    }

    // 복수 추가상품 수량 변경
    public void updateCartItemAddonQuantity(CartItemAddon cartItemAddon, int quantity) {
        if (quantity == 0) {
            cartItemAddons.remove(cartItemAddon);
            return;
        }

        cartItemAddon.updateQuantity(quantity);
    }
}
