package com.earthy.shop.domain.member.repository;

import com.earthy.shop.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 회원 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 회원 이메일 기준 단건 조회
    Optional<Member> findByEmail(String email);

    // 활성 회원 이메일 기준 단건 조회
    Optional<Member> findByEmailAndActiveTrue(String email);
}
