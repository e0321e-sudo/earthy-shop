package com.earthy.shop.domain.product.repository;

import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.entity.ProductSizeOption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductSizeOptionRepository extends JpaRepository<ProductSizeOption, Long> {

    // 관리자용 상품 사이즈 옵션 조회
    List<ProductSizeOption> findByProductAndDeletedFalseOrderByIdAsc(Product product);

    // 고객용 활성 상품 사이즈 옵션 조회
    List<ProductSizeOption> findByProductAndActiveTrueAndDeletedFalseOrderByIdAsc(Product product);

    // 고객용 활성 상품 사이즈 옵션 단건 조회
    Optional<ProductSizeOption> findByIdAndProductAndActiveTrueAndDeletedFalse(Long id, Product product);

    // 활성 사이즈 옵션 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pso
            from ProductSizeOption pso
            where pso.id = :id
              and pso.active = true
              and pso.deleted = false
            """)
    Optional<ProductSizeOption> findActiveByIdForUpdate(@Param("id") Long id);
}
