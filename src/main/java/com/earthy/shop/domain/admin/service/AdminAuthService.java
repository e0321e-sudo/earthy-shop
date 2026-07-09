package com.earthy.shop.domain.admin.service;

import com.earthy.shop.common.config.JwtUtil;
import com.earthy.shop.common.enums.UserRole;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.admin.dto.request.AdminLoginRequestDto;
import com.earthy.shop.domain.admin.dto.response.AdminLoginResponseDto;
import com.earthy.shop.domain.admin.entity.Admin;
import com.earthy.shop.domain.admin.repository.AdminRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 관리자 로그인
    public AdminLoginResponseDto login(AdminLoginRequestDto requestDto) {
        Admin admin = adminRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        if (!passwordEncoder.matches(requestDto.getPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN);

        return new AdminLoginResponseDto(accessToken);
    }
}
