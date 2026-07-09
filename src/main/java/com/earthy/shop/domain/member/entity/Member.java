package com.earthy.shop.domain.member.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원 이메일
    @Column(nullable = false, unique = true)
    private String email;

    // 회원 비밀번호
    @Column(nullable = false)
    private String password;

    // 회원 이름
    @Column(nullable = false)
    private String name;

    // 회원 연락처
    @Column(nullable = false)
    private String phone;

    // 회원 권한
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.MEMBER;

    // 회원 활성 상태
    @Column(nullable = false)
    private boolean active = true;

    public Member(
            String email,
            String password,
            String name,
            String phone
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = UserRole.MEMBER;
        this.active = true;
    }
}
