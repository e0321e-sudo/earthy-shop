package com.earthy.shop.domain.product.repository;

import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 활성 상품 목록 조회 (고객용 전체 상품 조회)
    List<Product> findByActiveTrue();

    // 카테고리별 활성 상품 목록 조회 (고객용 카테고리 상품 조회)
    List<Product> findByCategoryAndActiveTrue(ProductCategory category);

    // 활성 상품 단건 조회 (고객용 상품 상세 조회)
    Optional<Product> findByIdAndActiveTrue(Long id);
}
