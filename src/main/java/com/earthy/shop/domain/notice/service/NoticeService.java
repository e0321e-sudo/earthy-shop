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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 고객 공지사항 목록 조회
    public PageResponseDto<NoticeResponseDto> getNotices(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return PageResponseDto.from(
                    noticeRepository.findByVisibleTrueOrderByCreatedAtDesc(pageable)
                            .map(NoticeResponseDto::from)
            );
        }

        return PageResponseDto.from(
                noticeRepository.searchVisibleByTitleOrContent(keyword, pageable)
                        .map(NoticeResponseDto::from)
        );
    }

    // 고객 공지사항 상세 조회
    public NoticeResponseDto getNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdAndVisibleTrue(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        return NoticeResponseDto.from(notice);
    }

    // 관리자 공지사항 목록 조회
    public PageResponseDto<NoticeResponseDto> getAdminNotices(
            String keyword,
            NoticeVisibilityFilter visibility,
            Pageable pageable
    ) {
        Boolean visible = resolveVisible(visibility);

        return PageResponseDto.from(
                noticeRepository.findAdminNotices(keyword, visible, pageable)
                        .map(NoticeResponseDto::from)
        );
    }

    // 관리자 공지사항 상세 조회
    public NoticeResponseDto getAdminNotice(Long noticeId) {
        Notice notice = getNoticeEntity(noticeId);

        return NoticeResponseDto.from(notice);
    }

    // 관리자 공지사항 등록
    @Transactional
    public NoticeResponseDto createNotice(NoticeCreateRequestDto requestDto) {
        Notice notice = new Notice(
                requestDto.getTitle(),
                requestDto.getContent()
        );

        Notice savedNotice = noticeRepository.save(notice);

        return NoticeResponseDto.from(savedNotice);
    }

    // 관리자 공지사항 수정
    @Transactional
    public NoticeResponseDto updateNotice(Long noticeId, NoticeUpdateRequestDto requestDto) {
        Notice notice = getNoticeEntity(noticeId);

        notice.update(
                requestDto.getTitle(),
                requestDto.getContent()
        );

        return NoticeResponseDto.from(notice);
    }

    // 관리자 공지사항 비공개 처리
    @Transactional
    public NoticeResponseDto hideNotice(Long noticeId) {
        Notice notice = getNoticeEntity(noticeId);

        notice.hide();

        return NoticeResponseDto.from(notice);
    }

    // 관리자 공지사항 공개 처리
    @Transactional
    public NoticeResponseDto showNotice(Long noticeId) {
        Notice notice = getNoticeEntity(noticeId);

        notice.show();

        return NoticeResponseDto.from(notice);
    }

    // 공지사항 엔티티 조회
    private Notice getNoticeEntity(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
    }

    // 관리자 공지사항 공개 여부 필터 변환
    private Boolean resolveVisible(NoticeVisibilityFilter visibility) {
        if (visibility == null || visibility == NoticeVisibilityFilter.ALL) {
            return null;
        }

        return visibility == NoticeVisibilityFilter.PUBLIC;
    }
}