package com.earthy.shop.domain.notice.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.notice.dto.request.NoticeCreateRequestDto;
import com.earthy.shop.domain.notice.dto.request.NoticeUpdateRequestDto;
import com.earthy.shop.domain.notice.dto.response.NoticeResponseDto;
import com.earthy.shop.domain.notice.entity.Notice;
import com.earthy.shop.domain.notice.enums.NoticeVisibilityFilter;
import com.earthy.shop.domain.notice.repository.NoticeRepository;
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    void 고객_공지사항_목록을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Notice notice = notice("배송비 및 교환 환불 공지", "공지 내용");

        given(noticeRepository.findByVisibleTrueOrderByCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of(notice), pageable, 1));

        // when
        PageResponseDto<NoticeResponseDto> response = noticeService.getNotices(null, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().title()).isEqualTo("배송비 및 교환 환불 공지");
    }

    @Test
    void 고객_공지사항을_검색한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Notice notice = notice("배송비 공지", "배송비 안내");

        given(noticeRepository.searchVisibleByTitleOrContent("배송비", pageable))
                .willReturn(new PageImpl<>(List.of(notice), pageable, 1));

        // when
        PageResponseDto<NoticeResponseDto> response = noticeService.getNotices("배송비", pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().title()).contains("배송비");
    }

    @Test
    void 고객_공지사항_상세를_조회한다() {
        // given
        Notice notice = notice("배송비 공지", "배송비 안내");

        given(noticeRepository.findByIdAndVisibleTrue(1L)).willReturn(Optional.of(notice));

        // when
        NoticeResponseDto response = noticeService.getNotice(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.visible()).isTrue();
    }

    @Test
    void 비공개_공지사항은_고객_상세_조회_시_예외가_발생한다() {
        // given
        given(noticeRepository.findByIdAndVisibleTrue(1L)).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> noticeService.getNotice(1L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND)
                );
    }

    @Test
    void 관리자가_공지사항을_등록한다() {
        // given
        NoticeCreateRequestDto requestDto = new NoticeCreateRequestDto("새 공지", "공지 내용");

        given(noticeRepository.save(any(Notice.class)))
                .willAnswer(invocation -> {
                    Notice notice = invocation.getArgument(0);
                    TestEntityUtils.setId(notice, 1L);
                    return notice;
                });

        // when
        NoticeResponseDto response = noticeService.createNotice(requestDto);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("새 공지");
        assertThat(response.visible()).isTrue();
    }

    @Test
    void 관리자가_공지사항을_수정한다() {
        // given
        Notice notice = notice("기존 공지", "기존 내용");
        NoticeUpdateRequestDto requestDto = new NoticeUpdateRequestDto("수정 공지", "수정 내용");

        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

        // when
        NoticeResponseDto response = noticeService.updateNotice(1L, requestDto);

        // then
        assertThat(response.title()).isEqualTo("수정 공지");
        assertThat(response.content()).isEqualTo("수정 내용");
    }

    @Test
    void 관리자가_공개여부_필터로_공지사항을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Notice notice = notice("공개 공지", "공지 내용");

        given(noticeRepository.findAdminNotices("", true, pageable))
                .willReturn(new PageImpl<>(List.of(notice), pageable, 1));

        // when
        PageResponseDto<NoticeResponseDto> response =
                noticeService.getAdminNotices("", NoticeVisibilityFilter.PUBLIC, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().visible()).isTrue();
    }

    @Test
    void 관리자가_공지사항을_비공개_처리한다() {
        // given
        Notice notice = notice("공개 공지", "공지 내용");

        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

        // when
        NoticeResponseDto response = noticeService.hideNotice(1L);

        // then
        assertThat(response.visible()).isFalse();
    }

    @Test
    void 관리자가_전체_공지사항을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Notice notice = notice("전체 공지", "공지 내용");

        given(noticeRepository.findAdminNotices(null, null, pageable))
                .willReturn(new PageImpl<>(List.of(notice), pageable, 1));

        // when
        PageResponseDto<NoticeResponseDto> response =
                noticeService.getAdminNotices(null, NoticeVisibilityFilter.ALL, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().title()).isEqualTo("전체 공지");
    }

    @Test
    void 관리자가_비공개_공지사항을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Notice notice = notice("비공개 공지", "공지 내용");
        notice.hide();

        given(noticeRepository.findAdminNotices("", false, pageable))
                .willReturn(new PageImpl<>(List.of(notice), pageable, 1));

        // when
        PageResponseDto<NoticeResponseDto> response =
                noticeService.getAdminNotices("", NoticeVisibilityFilter.PRIVATE, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().visible()).isFalse();
    }

    @Test
    void 관리자가_공지사항을_공개_처리한다() {
        // given
        Notice notice = notice("비공개 공지", "공지 내용");
        notice.hide();

        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

        // when
        NoticeResponseDto response = noticeService.showNotice(1L);

        // then
        assertThat(response.visible()).isTrue();
    }

    @Test
    void 없는_공지사항_수정_시_예외가_발생한다() {
        // given
        NoticeUpdateRequestDto requestDto = new NoticeUpdateRequestDto("수정 공지", "수정 내용");

        given(noticeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> noticeService.updateNotice(1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND)
                );
    }

    @Test
    void 없는_공지사항_비공개_처리_시_예외가_발생한다() {
        // given
        given(noticeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> noticeService.hideNotice(1L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND)
                );
    }

    @Test
    void 없는_공지사항_공개_처리_시_예외가_발생한다() {
        // given
        given(noticeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> noticeService.showNotice(1L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND)
                );
    }

    private Notice notice(String title, String content) {
        Notice notice = new Notice(title, content);
        TestEntityUtils.setId(notice, 1L);
        return notice;
    }
}
