package com.earthy.shop.domain.member.service;

import com.earthy.shop.common.enums.LoginProvider;
import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.member.dto.request.MemberPasswordUpdateRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberSignupRequestDto;
import com.earthy.shop.domain.member.dto.request.MemberUpdateRequestDto;
import com.earthy.shop.domain.member.dto.response.AdminMemberResponseDto;
import com.earthy.shop.domain.member.dto.response.MemberResponseDto;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.enums.MemberStatusFilter;
import com.earthy.shop.domain.member.repository.MemberRepository;
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 회원가입을_한다() {
        // given
        MemberSignupRequestDto requestDto = new MemberSignupRequestDto(
                "user@example.com",
                "password123!",
                "박수지",
                "010-1234-5678",
                true,
                true,
                false
        );

        given(memberRepository.existsByEmail("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123!")).willReturn("encoded-password");
        given(memberRepository.save(any(Member.class)))
                .willAnswer(invocation -> {
                    Member member = invocation.getArgument(0);
                    TestEntityUtils.setId(member, 1L);
                    return member;
                });

        // when
        MemberResponseDto response = memberService.signup(requestDto);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("박수지");
        assertThat(response.provider()).isEqualTo(LoginProvider.LOCAL);
    }

    @Test
    void 필수약관에_동의하지_않으면_회원가입_예외가_발생한다() {
        // given
        MemberSignupRequestDto requestDto = new MemberSignupRequestDto(
                "user@example.com",
                "password123!",
                "박수지",
                "010-1234-5678",
                false,
                true,
                false
        );

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberService.signup(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUIRED_TERMS_NOT_AGREED)
                );
    }

    @Test
    void 중복_이메일이면_회원가입_예외가_발생한다() {
        // given
        MemberSignupRequestDto requestDto = new MemberSignupRequestDto(
                "user@example.com",
                "password123!",
                "박수지",
                "010-1234-5678",
                true,
                true,
                false
        );

        given(memberRepository.existsByEmail("user@example.com")).willReturn(true);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberService.signup(requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL)
                );
    }

    @Test
    void 내_정보를_조회한다() {
        // given
        Member member = member();

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));

        // when
        MemberResponseDto response = memberService.getMyInfo("user@example.com");

        // then
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("박수지");
    }

    @Test
    void 내_정보를_수정한다() {
        // given
        Member member = member();
        MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
                "이순신",
                "010-9999-8888",
                "51100",
                "경남 창원시 소답동",
                "711호"
        );

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));

        // when
        MemberResponseDto response = memberService.updateMyInfo("user@example.com", requestDto);

        // then
        assertThat(response.name()).isEqualTo("이순신");
        assertThat(response.phone()).isEqualTo("010-9999-8888");
        assertThat(response.zipCode()).isEqualTo("51100");
        assertThat(response.address()).isEqualTo("경남 창원시 소답동");
        assertThat(response.detailAddress()).isEqualTo("711호");
    }

    @Test
    void 첫_주문_연락처와_주소가_비어있으면_등록한다() {
        // given
        Member member = Member.createKakaoMember("kakao_1@earthy.local", "카카오회원", "1");

        // when
        memberService.registerOrderContactIfBlank(
                member,
                "010-1234-5678",
                "51100",
                "경남 창원시 소답동",
                "711호"
        );

        // then
        assertThat(member.getPhone()).isEqualTo("010-1234-5678");
        assertThat(member.getZipCode()).isEqualTo("51100");
        assertThat(member.getAddress()).isEqualTo("경남 창원시 소답동");
        assertThat(member.getDetailAddress()).isEqualTo("711호");
    }

    @Test
    void 첫_주문_연락처와_주소가_이미_있으면_덮어쓰지_않는다() {
        // given
        Member member = member();
        member.updateInfo("박수지", "010-1111-2222", "13529", "경기 성남시", "102호");

        // when
        memberService.registerOrderContactIfBlank(
                member,
                "010-9999-8888",
                "51100",
                "경남 창원시 소답동",
                "711호"
        );

        // then
        assertThat(member.getPhone()).isEqualTo("010-1111-2222");
        assertThat(member.getZipCode()).isEqualTo("13529");
        assertThat(member.getAddress()).isEqualTo("경기 성남시");
        assertThat(member.getDetailAddress()).isEqualTo("102호");
    }

    @Test
    void 회원_비밀번호를_변경한다() {
        // given
        Member member = member();
        MemberPasswordUpdateRequestDto requestDto = new MemberPasswordUpdateRequestDto(
                "old-password",
                "new-password"
        );

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("old-password", "encoded-password")).willReturn(true);
        given(passwordEncoder.matches("new-password", "encoded-password")).willReturn(false);
        given(passwordEncoder.encode("new-password")).willReturn("new-encoded-password");

        // when
        memberService.updatePassword("user@example.com", requestDto);

        // then
        assertThat(member.getPassword()).isEqualTo("new-encoded-password");
    }

    @Test
    void 현재_비밀번호가_틀리면_회원_비밀번호_변경_예외가_발생한다() {
        // given
        Member member = member();
        MemberPasswordUpdateRequestDto requestDto = new MemberPasswordUpdateRequestDto(
                "wrong-password",
                "new-password"
        );

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberService.updatePassword("user@example.com", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD)
                );
    }

    @Test
    void 기존_비밀번호와_같으면_회원_비밀번호_변경_예외가_발생한다() {
        // given
        Member member = member();
        MemberPasswordUpdateRequestDto requestDto = new MemberPasswordUpdateRequestDto(
                "old-password",
                "old-password"
        );

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("old-password", "encoded-password")).willReturn(true);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberService.updatePassword("user@example.com", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SAME_AS_OLD_PASSWORD)
                );
    }

    @Test
    void 소셜_회원은_비밀번호를_변경할_수_없다() {
        // given
        Member member = Member.createKakaoMember("kakao_1@earthy.local", "카카오회원", "1");
        MemberPasswordUpdateRequestDto requestDto = new MemberPasswordUpdateRequestDto(
                "old-password",
                "new-password"
        );

        given(memberRepository.findByEmailAndActiveTrue("kakao_1@earthy.local")).willReturn(Optional.of(member));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> memberService.updatePassword("kakao_1@earthy.local", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_MEMBER_PASSWORD_UNSUPPORTED)
                );
    }

    @Test
    void 회원을_탈퇴처리한다() {
        // given
        Member member = member();

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));

        // when
        memberService.deactivateMember("user@example.com");

        // then
        assertThat(member.isActive()).isFalse();
    }

    @Test
    void 관리자_회원_목록을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Member member = member();

        given(memberRepository.findAdminMembers(null, pageable))
                .willReturn(new PageImpl<>(List.of(member), pageable, 1));

        // when
        PageResponseDto<AdminMemberResponseDto> response = memberService.getAdminMembers(
                MemberStatusFilter.ALL,
                pageable
        );

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(memberRepository).findAdminMembers(null, pageable);
    }

    @Test
    void 관리자_활성_회원_목록을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Member member = member();

        given(memberRepository.findAdminMembers(true, pageable))
                .willReturn(new PageImpl<>(List.of(member), pageable, 1));

        // when
        PageResponseDto<AdminMemberResponseDto> response = memberService.getAdminMembers(
                MemberStatusFilter.ACTIVE,
                pageable
        );

        // then
        assertThat(response.content()).hasSize(1);
        verify(memberRepository).findAdminMembers(true, pageable);
    }

    @Test
    void 관리자_회원_상세를_조회한다() {
        // given
        Member member = member();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        AdminMemberResponseDto response = memberService.getAdminMember(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    void 활성_회원_엔티티를_조회한다() {
        // given
        Member member = member();

        given(memberRepository.findByEmailAndActiveTrue("user@example.com")).willReturn(Optional.of(member));

        // when
        Member response = memberService.getActiveMember("user@example.com");

        // then
        assertThat(response).isEqualTo(member);
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
