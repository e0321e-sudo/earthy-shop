package com.earthy.shop.domain.admin.repository;

import com.earthy.shop.domain.admin.entity.Admin;
import com.earthy.shop.domain.admin.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {

    // 관리자 기준 리프레시 토큰 조회
    Optional<AdminRefreshToken> findByAdmin(Admin admin);

    // 토큰 기준 리프레시 토큰 조회
    Optional<AdminRefreshToken> findByToken(String token);

    // 토큰 기준 리프레시 토큰 삭제
    void deleteByToken(String token);
}
