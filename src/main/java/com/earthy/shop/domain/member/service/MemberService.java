package com.earthy.shop.domain.member.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.member.dto.request.MemberPasswordUpdateRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberSignupRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberUpdateRequestDto;
import com.earthy.shop.domain.member.dto.response.MemberResponseDto;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
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
                requestDto.getPhone()
        );

        return MemberResponseDto.from(member);
    }

    // 회원 비밀번호 변경
    @Transactional
    public void updatePassword(String email, MemberPasswordUpdateRequestDto requestDto) {
        Member member = memberRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

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
}
