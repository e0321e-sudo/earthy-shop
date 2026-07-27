package com.earthy.shop.domain.member.repository;

import com.earthy.shop.common.enums.LoginProvider;
import com.earthy.shop.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 회원 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 활성 회원 이메일 기준 단건 조회
    Optional<Member> findByEmailAndActiveTrue(String email);

    // 관리자 회원 목록 조회
    @Query("""
        select m
        from Member m
        where (:active is null or m.active = :active)
        order by m.createdAt desc
        """)
    Page<Member> findAdminMembers(
            @Param("active") Boolean active,
            Pageable pageable
    );

    // 소셜 로그인 제공자 회원 ID 기준 조회
    Optional<Member> findByProviderAndProviderId(LoginProvider provider, String providerId);

    // 로그인 제공자와 이메일 기준 조회
    Optional<Member> findByEmailAndProvider(String email, LoginProvider provider);
}
