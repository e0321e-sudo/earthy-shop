package com.earthy.shop.domain.member.service;

import com.earthy.shop.common.config.JwtUtil;
import com.earthy.shop.common.enums.LoginProvider;
import com.earthy.shop.common.enums.UserRole;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.member.dto.request.MemberEmailFindRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberLoginRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberLogoutRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberPasswordFindRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberTokenRefreshRequestDto;
import com.earthy.shop.domain.member.dto.response.MemberEmailFindResponseDto;
import com.earthy.shop.domain.member.dto.response.MemberLoginResponseDto;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.entity.RefreshToken;
import com.earthy.shop.domain.member.repository.MemberRepository;
import com.earthy.shop.domain.member.repository.RefreshTokenRepository;
import com.earthy.shop.domain.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

// 회원 인증 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private static final String TEMP_PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    // 이메일 찾기
    public MemberEmailFindResponseDto findEmail(MemberEmailFindRequestDto requestDto) {
        // 이름과 연락처 기준 활성 회원 목록 조회
        List<Member> members = memberRepository.findAllByNameAndPhoneAndActiveTrue(
                requestDto.getName(),
                requestDto.getPhone()
        );

        if (members.isEmpty()) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_NOT_FOUND);
        }

        return MemberEmailFindResponseDto.from(members);
    }

    // 비밀번호 찾기
    @Transactional
    public void findPassword(MemberPasswordFindRequestDto requestDto) {
        // 이메일 기준 활성 회원 조회
        Member member = memberRepository.findByEmailAndActiveTrue(requestDto.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_PASSWORD_FIND_NOT_FOUND));

        // 소셜 로그인 회원 비밀번호 변경 방지
        LoginProvider provider = member.getProvider() == null
                ? LoginProvider.LOCAL
                : member.getProvider();

        if (provider != LoginProvider.LOCAL) {
            throw new BusinessException(ErrorCode.SOCIAL_MEMBER_PASSWORD_UNSUPPORTED);
        }

        // 임시비밀번호 생성
        String temporaryPassword = createTemporaryPassword();

        // 임시비밀번호 암호화 저장
        member.updatePassword(passwordEncoder.encode(temporaryPassword));

        // 임시비밀번호 이메일 발송
        emailService.sendTemporaryPassword(member.getEmail(), temporaryPassword);
    }

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

    // 임시비밀번호 생성
    private String createTemporaryPassword() {
        StringBuilder temporaryPassword = new StringBuilder();

        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARACTERS.length());
            temporaryPassword.append(TEMP_PASSWORD_CHARACTERS.charAt(index));
        }

        return temporaryPassword.toString();
    }
}
