package com.earthy.shop.domain.member.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // 리프레시 토큰
    @Column(nullable = false, length = 500)
    private String token;

    public RefreshToken(Member member, String token) {
        this.member = member;
        this.token = token;
    }

    // 리프레시 토큰 갱신
    public void updateToken(String token) {
        this.token = token;
    }
}
