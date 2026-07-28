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
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private OAuthService oAuthService;

    @Test
    void 기존_카카오_회원으로_로그인한다() {
        // given
        Member member = kakaoMember();

        given(kakaoOAuthClient.requestToken("auth-code")).willReturn(kakaoTokenResponse());
        given(kakaoOAuthClient.requestUser("kakao-access-token")).willReturn(kakaoUserResponse("kakao@example.com"));
        given(memberRepository.findByProviderAndProviderId(LoginProvider.KAKAO, "5001"))
                .willReturn(Optional.of(member));
        given(jwtUtil.generateToken("kakao@example.com", UserRole.MEMBER)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("kakao@example.com", UserRole.MEMBER)).willReturn("refresh-token");
        given(refreshTokenRepository.findByMember(member)).willReturn(Optional.empty());

        // when
        MemberLoginResponseDto response = oAuthService.loginWithKakao("auth-code");

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 신규_카카오_회원이면_자동가입_후_로그인한다() {
        // given
        Member savedMember = kakaoMember();

        given(kakaoOAuthClient.requestToken("auth-code")).willReturn(kakaoTokenResponse());
        given(kakaoOAuthClient.requestUser("kakao-access-token")).willReturn(kakaoUserResponse("kakao@example.com"));
        given(memberRepository.findByProviderAndProviderId(LoginProvider.KAKAO, "5001"))
                .willReturn(Optional.empty());
        given(memberRepository.existsByEmail("kakao@example.com")).willReturn(false);
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);
        given(jwtUtil.generateToken("kakao@example.com", UserRole.MEMBER)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("kakao@example.com", UserRole.MEMBER)).willReturn("refresh-token");
        given(refreshTokenRepository.findByMember(savedMember)).willReturn(Optional.empty());

        // when
        MemberLoginResponseDto response = oAuthService.loginWithKakao("auth-code");

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void 카카오_이메일이_없으면_내부_이메일로_자동가입한다() {
        // given
        Member savedMember = Member.createKakaoMember(
                "kakao_5001@earthy.local",
                "카카오회원",
                "5001"
        );
        TestEntityUtils.setId(savedMember, 1L);

        given(kakaoOAuthClient.requestToken("auth-code")).willReturn(kakaoTokenResponse());
        given(kakaoOAuthClient.requestUser("kakao-access-token")).willReturn(kakaoUserResponse(null));
        given(memberRepository.findByProviderAndProviderId(LoginProvider.KAKAO, "5001"))
                .willReturn(Optional.empty());
        given(memberRepository.existsByEmail("kakao_5001@earthy.local")).willReturn(false);
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);
        given(jwtUtil.generateToken("kakao_5001@earthy.local", UserRole.MEMBER)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("kakao_5001@earthy.local", UserRole.MEMBER)).willReturn("refresh-token");
        given(refreshTokenRepository.findByMember(savedMember)).willReturn(Optional.empty());

        // when
        MemberLoginResponseDto response = oAuthService.loginWithKakao("auth-code");

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(memberRepository).existsByEmail("kakao_5001@earthy.local");
    }

    @Test
    void 탈퇴한_카카오_회원이면_재활성화_후_로그인한다() {
        // given
        Member member = kakaoMember();
        member.deactivate();

        given(kakaoOAuthClient.requestToken("auth-code")).willReturn(kakaoTokenResponse());
        given(kakaoOAuthClient.requestUser("kakao-access-token")).willReturn(kakaoUserResponse("kakao@example.com"));
        given(memberRepository.findByProviderAndProviderId(LoginProvider.KAKAO, "5001"))
                .willReturn(Optional.of(member));
        given(jwtUtil.generateToken("kakao@example.com", UserRole.MEMBER)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("kakao@example.com", UserRole.MEMBER)).willReturn("refresh-token");
        given(refreshTokenRepository.findByMember(member)).willReturn(Optional.empty());

        // when
        oAuthService.loginWithKakao("auth-code");

        // then
        assertThat(member.isActive()).isTrue();
    }

    @Test
    void 기존_리프레시토큰이_있으면_카카오_로그인_시_토큰을_갱신한다() {
        // given
        Member member = kakaoMember();
        RefreshToken savedRefreshToken = new RefreshToken(member, "old-refresh-token");

        given(kakaoOAuthClient.requestToken("auth-code")).willReturn(kakaoTokenResponse());
        given(kakaoOAuthClient.requestUser("kakao-access-token")).willReturn(kakaoUserResponse("kakao@example.com"));
        given(memberRepository.findByProviderAndProviderId(LoginProvider.KAKAO, "5001"))
                .willReturn(Optional.of(member));
        given(jwtUtil.generateToken("kakao@example.com", UserRole.MEMBER)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("kakao@example.com", UserRole.MEMBER)).willReturn("new-refresh-token");
        given(refreshTokenRepository.findByMember(member)).willReturn(Optional.of(savedRefreshToken));

        // when
        MemberLoginResponseDto response = oAuthService.loginWithKakao("auth-code");

        // then
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(savedRefreshToken.getToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void 카카오_자동가입_이메일이_이미_존재하면_예외가_발생한다() {
        // given
        given(kakaoOAuthClient.requestToken("auth-code")).willReturn(kakaoTokenResponse());
        given(kakaoOAuthClient.requestUser("kakao-access-token")).willReturn(kakaoUserResponse("user@example.com"));
        given(memberRepository.findByProviderAndProviderId(LoginProvider.KAKAO, "5001"))
                .willReturn(Optional.empty());
        given(memberRepository.existsByEmail("user@example.com")).willReturn(true);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> oAuthService.loginWithKakao("auth-code"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL)
                );
    }

    private KakaoTokenResponseDto kakaoTokenResponse() {
        return new KakaoTokenResponseDto(
                "kakao-access-token",
                "kakao-refresh-token",
                "bearer",
                3600
        );
    }

    private KakaoUserResponseDto kakaoUserResponse(String email) {
        return new KakaoUserResponseDto(
                5001L,
                new KakaoUserResponseDto.KakaoAccount(
                        email,
                        new KakaoUserResponseDto.Profile("카카오회원")
                ),
                new KakaoUserResponseDto.Properties("카카오회원")
        );
    }

    private Member kakaoMember() {
        Member member = Member.createKakaoMember(
                "kakao@example.com",
                "카카오회원",
                "5001"
        );
        TestEntityUtils.setId(member, 1L);
        return member;
    }
}
