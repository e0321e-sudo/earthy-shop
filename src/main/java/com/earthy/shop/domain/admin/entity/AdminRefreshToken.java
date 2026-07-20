package com.earthy.shop.domain.admin.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admin_refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관리자
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false, unique = true)
    private Admin admin;

    // 리프레시 토큰
    @Column(nullable = false, length = 500)
    private String token;

    public AdminRefreshToken(Admin admin, String token) {
        this.admin = admin;
        this.token = token;
    }

    // 리프레시 토큰 갱신
    public void updateToken(String token) {
        this.token = token;
    }
}
