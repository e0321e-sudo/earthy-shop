package com.earthy.shop.domain.oauth.service;

import com.earthy.shop.common.config.JwtUtil;
import com.earthy.shop.common.enums.LoginProvider;
import com.earthy.shop.common.enums.UserRole;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.member.dto.response.MemberLoginResponseDto;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.entity.RefreshToken;
import com.earthy.shop.domain.member.repository.MemberRepository;
import com.earthy.shop.domain.member.repository.RefreshTokenRepository;
import com.earthy.shop.domain.oauth.client.KakaoOAuthClient;
import com.earthy.shop.domain.oauth.dto.KakaoTokenResponseDto;
import com.earthy.shop.domain.oauth.dto.KakaoUserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    // 카카오 로그인
    @Transactional
    public MemberLoginResponseDto loginWithKakao(String code) {
        // 카카오 토큰 요청
        KakaoTokenResponseDto tokenResponse = kakaoOAuthClient.requestToken(code);

        // 카카오 사용자 정보 요청
        KakaoUserResponseDto userResponse = kakaoOAuthClient.requestUser(tokenResponse.accessToken());

        // 카카오 회원 ID 추출
        String providerId = String.valueOf(userResponse.id());

        // 카카오 이메일 추출
        String email = resolveEmail(userResponse.getEmail(), providerId);

        // 카카오 닉네임 추출
        String nickname = userResponse.getNickname();

        // 카카오 회원 조회 또는 자동 가입
        Member member = memberRepository.findByProviderAndProviderId(LoginProvider.KAKAO, providerId)
                .orElseGet(() -> signupKakaoMember(email, nickname, providerId));

        // 탈퇴 카카오 회원 재가입 처리
        if (!member.isActive()) {
            member.reactivate();
        }

        // EARTHY 액세스 토큰 생성
        String accessToken = jwtUtil.generateToken(member.getEmail(), UserRole.MEMBER);

        // EARTHY 리프레시 토큰 생성
        String refreshToken = jwtUtil.generateRefreshToken(member.getEmail(), UserRole.MEMBER);

        // 리프레시 토큰 저장 또는 갱신
        refreshTokenRepository.findByMember(member)
                .ifPresentOrElse(
                        savedToken -> savedToken.updateToken(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(member, refreshToken))
                );

        return new MemberLoginResponseDto(accessToken, refreshToken);
    }

    // 카카오 이메일 계산
    private String resolveEmail(String email, String providerId) {
        // 이메일 제공 시 카카오 이메일 사용
        if (email != null && !email.isBlank()) {
            return email;
        }

        // 이메일 미제공 시 내부 식별용 이메일 생성
        return "kakao_" + providerId + "@earthy.local";
    }

    // 카카오 회원 자동 가입
    private Member signupKakaoMember(String email, String nickname, String providerId) {
        // 일반 회원 이메일 중복 검증
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.createKakaoMember(
                email,
                nickname,
                providerId
        );

        return memberRepository.save(member);
    }
}
