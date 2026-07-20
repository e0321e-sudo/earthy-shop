package com.earthy.shop.domain.product.repository;

import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 활성 상품 목록 조회 (고객용 전체 상품 조회)
    Page<Product> findByActiveTrueAndDeletedFalse(Pageable pageable);

    // 카테고리별 활성 상품 목록 조회 (고객용 카테고리 상품 조회)
    Page<Product> findByCategoryAndActiveTrueAndDeletedFalse(ProductCategory category, Pageable pageable);

    // 활성 상품 단건 조회 (고객용 상품 상세 조회)
    Optional<Product> findByIdAndActiveTrueAndDeletedFalse(Long id);

    // 삭제되지 않은 상품 목록 조회 (관리자용 상품 조회)
    Page<Product> findByDeletedFalse(Pageable pageable);
}
