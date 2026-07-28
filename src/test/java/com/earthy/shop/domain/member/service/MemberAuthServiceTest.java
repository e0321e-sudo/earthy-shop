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
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private MemberAuthService memberAuthService;

    @Test
    void 회원_로그인_시_토큰을_발급하고_리프레시토큰을_저장한다() {
        // given
        Member member = member();
        MemberLoginRequestDto requestDto = new MemberLoginRequestDto("user@example.com", "password");

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken("user@example.com", UserRole.MEMBER)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("user@example.com", UserRole.MEMBER)).willReturn("refresh-token");
        given(refreshTokenRepository.findByMember(member)).willReturn(Optional.empty());

        // when
        MemberLoginResponseDto response = memberAuthService.login(requestDto);

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 기존_리프레시토큰이_있으면_회원_로그인_시_토큰을_갱신한다() {
        // given
        Member member = member();
        RefreshToken savedRefreshToken = new RefreshToken(member, "old-refresh-token");
        MemberLoginRequestDto requestDto = new MemberLoginRequestDto("user@example.com", "password");

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken("user@example.com", UserRole.MEMBER)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("user@example.com", UserRole.MEMBER)).willReturn("new-refresh-token");
        given(refreshTokenRepository.findByMember(member)).willReturn(Optional.of(savedRefreshToken));

        // when
        MemberLoginResponseDto response = memberAuthService.login(requestDto);

        // then
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(savedRefreshToken.getToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void 회원이_없으면_로그인_예외가_발생한다() {
        // given
        MemberLoginRequestDto requestDto = new MemberLoginRequestDto("user@example.com", "password");

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberAuthService.login(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
                );
    }

    @Test
    void 비밀번호가_틀리면_로그인_예외가_발생한다() {
        // given
        Member member = member();
        MemberLoginRequestDto requestDto = new MemberLoginRequestDto("user@example.com", "wrong-password");

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberAuthService.login(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD)
                );
    }

    @Test
    void 회원_로그아웃_시_리프레시토큰을_삭제한다() {
        // given
        RefreshToken refreshToken = new RefreshToken(member(), "refresh-token");
        MemberLogoutRequestDto requestDto = new MemberLogoutRequestDto("refresh-token");

        given(jwtUtil.isValid("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByToken("refresh-token")).willReturn(Optional.of(refreshToken));

        // when
        memberAuthService.logout(requestDto);

        // then
        verify(refreshTokenRepository).deleteByToken("refresh-token");
    }

    @Test
    void 유효하지_않은_리프레시토큰으로_로그아웃하면_예외가_발생한다() {
        // given
        MemberLogoutRequestDto requestDto = new MemberLogoutRequestDto("invalid-refresh-token");

        given(jwtUtil.isValid("invalid-refresh-token")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberAuthService.logout(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN)
                );
    }

    @Test
    void 저장된_토큰이_없으면_로그아웃_예외가_발생한다() {
        // given
        MemberLogoutRequestDto requestDto = new MemberLogoutRequestDto("refresh-token");

        given(jwtUtil.isValid("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByToken("refresh-token")).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberAuthService.logout(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
                );
    }

    @Test
    void 회원_액세스토큰을_재발급한다() {
        // given
        Member member = member();
        RefreshToken savedRefreshToken = new RefreshToken(member, "old-refresh-token");
        MemberTokenRefreshRequestDto requestDto = new MemberTokenRefreshRequestDto("old-refresh-token");

        given(jwtUtil.isValid("old-refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByToken("old-refresh-token")).willReturn(Optional.of(savedRefreshToken));
        given(jwtUtil.generateToken("user@example.com", UserRole.MEMBER)).willReturn("new-access-token");
        given(jwtUtil.generateRefreshToken("user@example.com", UserRole.MEMBER)).willReturn("new-refresh-token");

        // when
        MemberLoginResponseDto response = memberAuthService.refreshToken(requestDto);

        // then
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(savedRefreshToken.getToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void 비활성_회원은_토큰을_재발급할_수_없다() {
        // given
        Member member = member();
        member.deactivate();
        RefreshToken savedRefreshToken = new RefreshToken(member, "refresh-token");
        MemberTokenRefreshRequestDto requestDto = new MemberTokenRefreshRequestDto("refresh-token");

        given(jwtUtil.isValid("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByToken("refresh-token")).willReturn(Optional.of(savedRefreshToken));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberAuthService.refreshToken(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
                );
    }

    private Member member() {
        Member member = new Member(
                "user@example.com",
                "encoded-password",
                "박수지",
                "010-1234-5678"
        );
        TestEntityUtils.setId(member, 1L);
        return member;
    }
}
