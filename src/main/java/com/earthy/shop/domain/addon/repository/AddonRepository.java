package com.earthy.shop.domain.addon.repository;

import com.earthy.shop.domain.addon.entity.Addon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddonRepository extends JpaRepository<Addon, Long> {

    // 활성 추가상품 목록 조회 (고객용 전체 추가상품 조회)
    List<Addon> findByActiveTrueAndDeletedFalse();

    // 활성 추가상품 단건 조회
    Optional<Addon> findByIdAndActiveTrueAndDeletedFalse(Long addonId);

    // 활성 추가상품 단건 잠금 조회 (재고 차감용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from Addon a
            where a.id = :addonId
              and a.active = true
              and a.deleted = false
            """)
    Optional<Addon> findActiveByIdForUpdate(@Param("addonId") Long addonId);

    // 삭제되지 않은 추가상품 목록 조회 (관리자용 추가상품 조회)
    Page<Addon> findByDeletedFalse(Pageable pageable);
}
