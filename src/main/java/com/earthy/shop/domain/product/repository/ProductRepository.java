package com.earthy.shop.domain.product.repository;

import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.product.enums.ProductCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 활성 상품 목록 조회 (고객용 전체 상품 조회)
    Page<Product> findByActiveTrueAndDeletedFalse(Pageable pageable);

    // 카테고리별 활성 상품 목록 조회 (고객용 카테고리 상품 조회)
    Page<Product> findByCategoryAndActiveTrueAndDeletedFalse(ProductCategory category, Pageable pageable);

    // 실제 판매수량 기준 인기 상품 목록 조회
    @Query(
            value = """
                    select p
                    from Product p
                    left join OrderItem oi on oi.productId = p.id
                    left join oi.order o on o.status in :salesStatuses
                    where p.active = true
                      and p.deleted = false
                      and (:category is null or p.category = :category)
                    group by p
                    order by coalesce(sum(case when o.id is not null then oi.quantity else 0 end), 0) desc,
                             p.createdAt desc,
                             p.id desc
                    """,
            countQuery = """
                    select count(p)
                    from Product p
                    where p.active = true
                      and p.deleted = false
                      and (:category is null or p.category = :category)
                    """
    )
    Page<Product> findPopularProducts(
            @Param("category") ProductCategory category,
            @Param("salesStatuses") Collection<OrderStatus> salesStatuses,
            Pageable pageable
    );

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
