package com.earthy.shop.domain.notice.repository;

import com.earthy.shop.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 고객 공지사항 목록 조회
    Page<Notice> findByVisibleTrueOrderByCreatedAtDesc(Pageable pageable);

    // 고객 공지사항 상세 조회
    Optional<Notice> findByIdAndVisibleTrue(Long noticeId);

    // 고객 공지사항 제목 또는 내용 검색
    @Query("""
            select n
            from Notice n
            where n.visible = true
              and (
                    n.title like concat('%', :keyword, '%')
                    or n.content like concat('%', :keyword, '%')
              )
            order by n.createdAt desc
            """)
    Page<Notice> searchVisibleByTitleOrContent(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 관리자 공지사항 제목 또는 내용 검색 및 공개 여부 필터 조회
    @Query("""
            select n
            from Notice n
            where (:visible is null or n.visible = :visible)
              and (
                    :keyword is null
                    or :keyword = ''
                    or n.title like concat('%', :keyword, '%')
                    or n.content like concat('%', :keyword, '%')
              )
            order by n.createdAt desc
            """)
    Page<Notice> findAdminNotices(
            @Param("keyword") String keyword,
            @Param("visible") Boolean visible,
            Pageable pageable
    );
}