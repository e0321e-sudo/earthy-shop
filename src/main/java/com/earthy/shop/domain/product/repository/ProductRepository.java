package com.earthy.shop.domain.product.repository;

import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 활성 상품 목록 조회 (고객용 전체 상품 조회)
    Page<Product> findByActiveTrueAndDeletedFalse(Pageable pageable);

    // 카테고리별 활성 상품 목록 조회 (고객용 카테고리 상품 조회)
    Page<Product> findByCategoryAndActiveTrueAndDeletedFalse(ProductCategory category, Pageable pageable);

    // 활성 상품 단건 조회 (고객용 상품 상세 조회)
    Optional<Product> findByIdAndActiveTrueAndDeletedFalse(Long id);

    // 활성 상품 단건 잠금 조회 (재고 차감용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Product p
            where p.id = :productId
              and p.active = true
              and p.deleted = false
            """)
    Optional<Product> findActiveByIdForUpdate(@Param("productId") Long productId);

    // 삭제되지 않은 상품 목록 조회 (관리자용 상품 조회)
    Page<Product> findByDeletedFalse(Pageable pageable);

    // 고객용 상품명 검색
    @Query("""
        select p
        from Product p
        where p.active = true
          and p.deleted = false
          and lower(p.name) like lower(concat('%', :keyword, '%'))
        order by p.createdAt desc
        """)
    Page<Product> searchByName(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
