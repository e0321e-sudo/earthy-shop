package com.earthy.shop.domain.member.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.common.enums.LoginProvider;
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

    // 회원 우편번호
    @Column
    private String zipCode;

    // 회원 기본주소
    @Column
    private String address;

    // 회원 상세주소
    @Column
    private String detailAddress;

    // 회원 권한
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.MEMBER;

    // 회원 활성 상태
    @Column(nullable = false)
    private boolean active = true;

    // 로그인 제공자
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginProvider provider = LoginProvider.LOCAL;

    // 소셜 로그인 제공자 회원 ID
    @Column
    private String providerId;

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

    // 카카오 회원 생성
    public static Member createKakaoMember(String email, String name, String providerId) {
        Member member = new Member();

        member.email = email;
        member.password = "";
        member.name = name;
        member.phone = "";
        member.role = UserRole.MEMBER;
        member.provider = LoginProvider.KAKAO;
        member.providerId = providerId;
        member.active = true;

        return member;
    }

    // 회원 정보 수정
    public void updateInfo(String name, String phone, String zipCode, String address, String detailAddress) {
        this.name = name;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.detailAddress = detailAddress;
    }

    // 회원 연락처 최초 등록
    public void registerPhoneIfBlank(String phone) {
        if (this.phone != null && !this.phone.isBlank()) {
            return;
        }

        this.phone = phone;
    }

    // 회원 주소 최초 등록
    public void registerAddressIfBlank(String zipCode, String address, String detailAddress) {
        if (this.zipCode != null && !this.zipCode.isBlank()
                && this.address != null && !this.address.isBlank()) {
            return;
        }

        this.zipCode = zipCode;
        this.address = address;
        this.detailAddress = detailAddress;
    }

    // 회원 비밀번호 변경
    public void updatePassword(String password) {
        this.password = password;
    }

    // 회원 비활성화
    public void deactivate() {
        this.active = false;
    }

    // 회원 재활성화
    public void reactivate() {
        this.active = true;
    }
}
