package com.earthy.shop.domain.member.repository;

import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 회원 기준 리프레시 토큰 조회
    Optional<RefreshToken> findByMember(Member member);

    // 토큰 기준 리프레시 토큰 조회
    Optional<RefreshToken> findByToken(String token);

    // 회원 기준 리프레시 토큰 삭제
    void deleteByMember(Member member);

    // 토큰 기준 리프레시 토큰 삭제
    void deleteByToken(String token);
}
