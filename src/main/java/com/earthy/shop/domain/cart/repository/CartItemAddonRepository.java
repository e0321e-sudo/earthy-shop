package com.earthy.shop.domain.cart.repository;

import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.cart.entity.CartItem;
import com.earthy.shop.domain.cart.entity.CartItemAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemAddonRepository extends JpaRepository<CartItemAddon, Long> {

    // 장바구니 추가상품 단건 조회
    Optional<CartItemAddon> findByIdAndCartItem(Long cartItemAddonId, CartItem cartItem);

    // 추가상품이 담긴 장바구니 추가상품 삭제
    void deleteByAddon(Addon addon);
}
