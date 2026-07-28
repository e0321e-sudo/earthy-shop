package com.earthy.shop.domain.admin.service;

import com.earthy.shop.common.config.JwtUtil;
import com.earthy.shop.common.enums.UserRole;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.admin.dto.request.AdminLoginRequestDto;
import com.earthy.shop.domain.admin.dto.request.AdminLogoutRequestDto;
import com.earthy.shop.domain.admin.dto.request.AdminTokenRefreshRequestDto;
import com.earthy.shop.domain.admin.dto.response.AdminLoginResponseDto;
import com.earthy.shop.domain.admin.entity.Admin;
import com.earthy.shop.domain.admin.entity.AdminRefreshToken;
import com.earthy.shop.domain.admin.repository.AdminRefreshTokenRepository;
import com.earthy.shop.domain.admin.repository.AdminRepository;
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
class AdminAuthServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AdminRefreshTokenRepository adminRefreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    void 관리자_로그인_시_토큰을_발급하고_리프레시토큰을_저장한다() {
        // given
        Admin admin = admin();
        AdminLoginRequestDto requestDto = new AdminLoginRequestDto("admin@example.com", "password");

        given(adminRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("password", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken("admin@example.com", UserRole.ADMIN)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("admin@example.com", UserRole.ADMIN)).willReturn("refresh-token");
        given(adminRefreshTokenRepository.findByAdmin(admin)).willReturn(Optional.empty());

        // when
        AdminLoginResponseDto response = adminAuthService.login(requestDto);

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(adminRefreshTokenRepository).save(any(AdminRefreshToken.class));
    }

    @Test
    void 기존_리프레시토큰이_있으면_관리자_로그인_시_토큰을_갱신한다() {
        // given
        Admin admin = admin();
        AdminRefreshToken savedRefreshToken = new AdminRefreshToken(admin, "old-refresh-token");
        AdminLoginRequestDto requestDto = new AdminLoginRequestDto("admin@example.com", "password");

        given(adminRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("password", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken("admin@example.com", UserRole.ADMIN)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("admin@example.com", UserRole.ADMIN)).willReturn("new-refresh-token");
        given(adminRefreshTokenRepository.findByAdmin(admin)).willReturn(Optional.of(savedRefreshToken));

        // when
        AdminLoginResponseDto response = adminAuthService.login(requestDto);

        // then
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(savedRefreshToken.getToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void 관리자가_없으면_로그인_예외가_발생한다() {
        // given
        AdminLoginRequestDto requestDto = new AdminLoginRequestDto("admin@example.com", "password");

        given(adminRepository.findByEmail("admin@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> adminAuthService.login(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADMIN_NOT_FOUND)
                );
    }

    @Test
    void 관리자_비밀번호가_틀리면_로그인_예외가_발생한다() {
        // given
        Admin admin = admin();
        AdminLoginRequestDto requestDto = new AdminLoginRequestDto("admin@example.com", "wrong-password");

        given(adminRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> adminAuthService.login(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD)
                );
    }

    @Test
    void 관리자_로그아웃_시_리프레시토큰을_삭제한다() {
        // given
        AdminRefreshToken refreshToken = new AdminRefreshToken(admin(), "refresh-token");
        AdminLogoutRequestDto requestDto = new AdminLogoutRequestDto("refresh-token");

        given(jwtUtil.isValid("refresh-token")).willReturn(true);
        given(adminRefreshTokenRepository.findByToken("refresh-token")).willReturn(Optional.of(refreshToken));

        // when
        adminAuthService.logout(requestDto);

        // then
        verify(adminRefreshTokenRepository).deleteByToken("refresh-token");
    }

    @Test
    void 유효하지_않은_관리자_리프레시토큰으로_로그아웃하면_예외가_발생한다() {
        // given
        AdminLogoutRequestDto requestDto = new AdminLogoutRequestDto("invalid-refresh-token");

        given(jwtUtil.isValid("invalid-refresh-token")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> adminAuthService.logout(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN)
                );
    }

    @Test
    void 관리자_액세스토큰을_재발급한다() {
        // given
        Admin admin = admin();
        AdminRefreshToken savedRefreshToken = new AdminRefreshToken(admin, "old-refresh-token");
        AdminTokenRefreshRequestDto requestDto = new AdminTokenRefreshRequestDto("old-refresh-token");

        given(jwtUtil.isValid("old-refresh-token")).willReturn(true);
        given(adminRefreshTokenRepository.findByToken("old-refresh-token")).willReturn(Optional.of(savedRefreshToken));
        given(jwtUtil.generateToken("admin@example.com", UserRole.ADMIN)).willReturn("new-access-token");
        given(jwtUtil.generateRefreshToken("admin@example.com", UserRole.ADMIN)).willReturn("new-refresh-token");

        // when
        AdminLoginResponseDto response = adminAuthService.refreshToken(requestDto);

        // then
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(savedRefreshToken.getToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void 저장된_관리자_리프레시토큰이_없으면_재발급_예외가_발생한다() {
        // given
        AdminTokenRefreshRequestDto requestDto = new AdminTokenRefreshRequestDto("refresh-token");

        given(jwtUtil.isValid("refresh-token")).willReturn(true);
        given(adminRefreshTokenRepository.findByToken("refresh-token")).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> adminAuthService.refreshToken(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
                );
    }

    private Admin admin() {
        Admin admin = new Admin("admin@example.com", "encoded-password");
        TestEntityUtils.setId(admin, 1L);
        return admin;
    }
}
