package com.earthy.shop.domain.admin.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관리자 이메일
    @Column(nullable = false, unique = true)
    private String email;

    // 관리자 비밀번호
    @Column(nullable = false)
    private String password;

    public Admin(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // 관리자 비밀번호 변경
    public void updatePassword(String password) {
        this.password = password;
    }
}
