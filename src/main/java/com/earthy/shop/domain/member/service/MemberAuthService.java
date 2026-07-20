package com.earthy.shop.domain.member.service;

import com.earthy.shop.common.config.JwtUtil;
import com.earthy.shop.common.enums.UserRole;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.member.dto.request.MemberLoginRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberLogoutRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberTokenRefreshRequestDto;
import com.earthy.shop.domain.member.dto.response.MemberLoginResponseDto;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.entity.RefreshToken;
import com.earthy.shop.domain.member.repository.MemberRepository;
import com.earthy.shop.domain.member.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 인증 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원 로그인
    @Transactional
    public MemberLoginResponseDto login(MemberLoginRequestDto requestDto) {
        Member member = memberRepository.findByEmailAndActiveTrue(requestDto.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(requestDto.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.generateToken(member.getEmail(), UserRole.MEMBER);
        String refreshToken = jwtUtil.generateRefreshToken(member.getEmail(), UserRole.MEMBER);

        refreshTokenRepository.findByMember(member)
                .ifPresentOrElse(
                        savedToken -> savedToken.updateToken(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(member, refreshToken))
                );

        return new MemberLoginResponseDto(accessToken, refreshToken);
    }

    // 로그아웃
    @Transactional
    public void logout(MemberLogoutRequestDto requestDto) {
        String refreshToken = requestDto.getRefreshToken();

        if (!jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 리프레시 토큰 조회
        refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 리프레시 토큰 삭제
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    // 액세스 토큰 재발급
    @Transactional
    public MemberLoginResponseDto refreshToken(MemberTokenRefreshRequestDto requestDto) {
        String refreshToken = requestDto.getRefreshToken();

        if (!jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        Member member = savedRefreshToken.getMember();

        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        String newAccessToken = jwtUtil.generateToken(member.getEmail(), UserRole.MEMBER);
        String newRefreshToken = jwtUtil.generateRefreshToken(member.getEmail(), UserRole.MEMBER);

        savedRefreshToken.updateToken(newRefreshToken);

        return new MemberLoginResponseDto(newAccessToken, newRefreshToken);
    }
}
