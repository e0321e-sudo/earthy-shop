package com.earthy.shop.domain.addon.repository;

import com.earthy.shop.domain.addon.entity.Addon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddonRepository extends JpaRepository<Addon, Long> {

    // 활성 추가상품 목록 조회 (고객용 전체 추가상품 조회)
    List<Addon> findByActiveTrue();
}
