package com.earthy.shop.domain.addon.repository;

import com.earthy.shop.domain.addon.entity.Addon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddonRepository extends JpaRepository<Addon, Long> {

    // 활성 추가상품 목록 조회 (고객용 전체 추가상품 조회)
    List<Addon> findByActiveTrueAndDeletedFalse();

    // 활성 추가상품 단건 조회
    Optional<Addon> findByIdAndActiveTrueAndDeletedFalse(Long addonId);

    // 삭제되지 않은 추가상품 목록 조회 (관리자용 추가상품 조회)
    Page<Addon> findByDeletedFalse(Pageable pageable);
}
