package com.earthy.shop.domain.admin.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.admin.dto.request.AdminPasswordUpdateRequestDto;
import com.earthy.shop.domain.admin.entity.Admin;
import com.earthy.shop.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    // 관리자 비밀번호 변경
    @Transactional
    public void updatePassword(String email, AdminPasswordUpdateRequestDto requestDto) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        // 현재 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 기존 비밀번호와 동일 여부 확인
        if (passwordEncoder.matches(requestDto.getNewPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_AS_OLD_PASSWORD);
        }

        // 새 비밀번호 암호화 후 변경
        admin.updatePassword(passwordEncoder.encode(requestDto.getNewPassword()));
    }
}