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
import com.earthy.shop.domain.admin.repository.AdminRepository;
import com.earthy.shop.domain.admin.repository.AdminRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 인증 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 관리자 로그인
    @Transactional
    public AdminLoginResponseDto login(AdminLoginRequestDto requestDto) {
        Admin admin = adminRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        if (!passwordEncoder.matches(requestDto.getPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN);
        String refreshToken = jwtUtil.generateRefreshToken(admin.getEmail(), UserRole.ADMIN);

        adminRefreshTokenRepository.findByAdmin(admin)
                .ifPresentOrElse(
                        savedToken -> savedToken.updateToken(refreshToken),
                        () -> adminRefreshTokenRepository.save(new AdminRefreshToken(admin, refreshToken))
                );

        return new AdminLoginResponseDto(accessToken, refreshToken);
    }

    // 관리자 로그아웃
    @Transactional
    public void logout(AdminLogoutRequestDto requestDto) {
        String refreshToken = requestDto.getRefreshToken();

        if (!jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 리프레시 토큰 조회
        adminRefreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 리프레시 토큰 삭제
        adminRefreshTokenRepository.deleteByToken(refreshToken);
    }

    // 관리자 액세스 토큰 재발급
    @Transactional
    public AdminLoginResponseDto refreshToken(AdminTokenRefreshRequestDto requestDto) {
        String refreshToken = requestDto.getRefreshToken();

        if (!jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        AdminRefreshToken savedRefreshToken = adminRefreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        Admin admin = savedRefreshToken.getAdmin();
        String newAccessToken = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN);
        String newRefreshToken = jwtUtil.generateRefreshToken(admin.getEmail(), UserRole.ADMIN);

        savedRefreshToken.updateToken(newRefreshToken);

        return new AdminLoginResponseDto(newAccessToken, newRefreshToken);
    }
}
