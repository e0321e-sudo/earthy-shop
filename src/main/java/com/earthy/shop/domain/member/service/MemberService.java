package com.earthy.shop.domain.member.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.enums.LoginProvider;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.member.dto.request.MemberPasswordUpdateRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberSignupRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberUpdateRequestDto;
import com.earthy.shop.domain.member.dto.response.AdminMemberResponseDto;
import com.earthy.shop.domain.member.dto.response.MemberResponseDto;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.enums.MemberStatusFilter;
import com.earthy.shop.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public MemberResponseDto signup(MemberSignupRequestDto requestDto) {
        // 필수 약관 동의 검증
        if (!requestDto.isTermsAgreed() || !requestDto.isPrivacyAgreed()) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        if (memberRepository.existsByEmail(requestDto.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = new Member(
                requestDto.getEmail(),
                passwordEncoder.encode(requestDto.getPassword()),
                requestDto.getName(),
                requestDto.getPhone()
        );

        Member savedMember = memberRepository.save(member);

        return MemberResponseDto.from(savedMember);
    }

    // 회원 내 정보 조회
    public MemberResponseDto getMyInfo(String email) {
        Member member = memberRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponseDto.from(member);
    }

    // 회원 정보 수정
    @Transactional
    public MemberResponseDto updateMyInfo(String email, MemberUpdateRequestDto requestDto) {
        Member member = memberRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.updateInfo(
                requestDto.getName(),
                requestDto.getPhone(),
                requestDto.getZipCode(),
                requestDto.getAddress(),
                requestDto.getDetailAddress()
        );

        return MemberResponseDto.from(member);
    }

    // 첫 주문 회원 연락처 및 주소 등록
    @Transactional
    public void registerOrderContactIfBlank(
            Member member,
            String phone,
            String zipCode,
            String address,
            String detailAddress
    ) {
        member.registerPhoneIfBlank(phone);
        member.registerAddressIfBlank(zipCode, address, detailAddress);
    }

    // 회원 비밀번호 변경
    @Transactional
    public void updatePassword(String email, MemberPasswordUpdateRequestDto requestDto) {
        Member member = memberRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 비밀번호가 없는 소셜 로그인 회원 비밀번호 변경 제한
        if (member.getProvider() != null
                && member.getProvider() != LoginProvider.LOCAL
                && member.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_MEMBER_PASSWORD_UNSUPPORTED);
        }

        // 현재 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 기존 비밀번호와 동일 여부 확인
        if (passwordEncoder.matches(requestDto.getNewPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_AS_OLD_PASSWORD);
        }

        // 새 비밀번호 암호화 후 변경
        member.updatePassword(passwordEncoder.encode(requestDto.getNewPassword()));
    }

    // 회원 탈퇴
    @Transactional
    public void deactivateMember(String email) {
        Member member = memberRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.deactivate();
    }

    // 관리자 회원 목록 조회
    public PageResponseDto<AdminMemberResponseDto> getAdminMembers(
            MemberStatusFilter status,
            Pageable pageable
    ) {
        Boolean active = resolveActive(status);

        return PageResponseDto.from(
                memberRepository.findAdminMembers(active, pageable)
                        .map(AdminMemberResponseDto::from)
        );
    }

    // 관리자 회원 활성 상태 필터 변환
    private Boolean resolveActive(MemberStatusFilter status) {
        if (status == null || status == MemberStatusFilter.ALL) {
            return null;
        }

        return status == MemberStatusFilter.ACTIVE;
    }

    // 관리자 회원 상세 조회
    public AdminMemberResponseDto getAdminMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return AdminMemberResponseDto.from(member);
    }

    // 활성 회원 엔티티 조회
    @Transactional(readOnly = true)
    public Member getActiveMember(String email) {
        return memberRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
