package com.earthy.shop.domain.admin.repository;

import com.earthy.shop.domain.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    // 관리자 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 관리자 이메일 기준 단건 조회
    Optional<Admin> findByEmail(String email);
}
