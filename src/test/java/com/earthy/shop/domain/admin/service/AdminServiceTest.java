package com.earthy.shop.domain.admin.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.admin.dto.request.AdminPasswordUpdateRequestDto;
import com.earthy.shop.domain.admin.entity.Admin;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    @Test
    void 관리자_비밀번호를_변경한다() {
        // given
        Admin admin = admin();
        AdminPasswordUpdateRequestDto requestDto = new AdminPasswordUpdateRequestDto(
                "old-password",
                "new-password"
        );

        given(adminRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("old-password", "encoded-password")).willReturn(true);
        given(passwordEncoder.matches("new-password", "encoded-password")).willReturn(false);
        given(passwordEncoder.encode("new-password")).willReturn("new-encoded-password");

        // when
        adminService.updatePassword("admin@example.com", requestDto);

        // then
        assertThat(admin.getPassword()).isEqualTo("new-encoded-password");
    }

    @Test
    void 관리자가_없으면_비밀번호_변경_예외가_발생한다() {
        // given
        AdminPasswordUpdateRequestDto requestDto = new AdminPasswordUpdateRequestDto(
                "old-password",
                "new-password"
        );

        given(adminRepository.findByEmail("admin@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> adminService.updatePassword("admin@example.com", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADMIN_NOT_FOUND)
                );
    }

    @Test
    void 현재_비밀번호가_틀리면_관리자_비밀번호_변경_예외가_발생한다() {
        // given
        Admin admin = admin();
        AdminPasswordUpdateRequestDto requestDto = new AdminPasswordUpdateRequestDto(
                "wrong-password",
                "new-password"
        );

        given(adminRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> adminService.updatePassword("admin@example.com", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD)
                );
    }

    @Test
    void 기존_비밀번호와_같으면_관리자_비밀번호_변경_예외가_발생한다() {
        // given
        Admin admin = admin();
        AdminPasswordUpdateRequestDto requestDto = new AdminPasswordUpdateRequestDto(
                "old-password",
                "old-password"
        );

        given(adminRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("old-password", "encoded-password")).willReturn(true);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> adminService.updatePassword("admin@example.com", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SAME_AS_OLD_PASSWORD)
                );
    }

    private Admin admin() {
        Admin admin = new Admin("admin@example.com", "encoded-password");
        TestEntityUtils.setId(admin, 1L);
        return admin;
    }
}
