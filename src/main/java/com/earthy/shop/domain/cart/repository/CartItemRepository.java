package com.earthy.shop.domain.cart.repository;

import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.cart.entity.CartItem;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.entity.ProductSizeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 회원 장바구니 목록 조회
    List<CartItem> findByMember(Member member);

    // 회원 장바구니 상품 단건 조회
    Optional<CartItem> findByIdAndMember(Long cartItemId, Member member);

    // 회원 장바구니 동일 상품/사이즈 조회
    @Query("""
            select c
            from CartItem c
            where c.member = :member
              and c.product = :product
              and (
                    (:productSizeOption is null and c.productSizeOption is null)
                    or c.productSizeOption = :productSizeOption
                  )
            """)
    List<CartItem> findSameCartItems(
            @Param("member") Member member,
            @Param("product") Product product,
            @Param("productSizeOption") ProductSizeOption productSizeOption
    );

    // 회원 장바구니 전체 삭제
    void deleteByMember(Member member);

    // 상품이 담긴 장바구니 항목 삭제
    void deleteByProduct(Product product);

    // 기존 단일 추가상품이 담긴 장바구니 항목 삭제
    void deleteByAddon(Addon addon);

}
