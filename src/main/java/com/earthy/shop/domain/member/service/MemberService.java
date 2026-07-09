package com.earthy.shop.domain.member.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.member.dto.request.MemberSignupRequestDto;
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
}
