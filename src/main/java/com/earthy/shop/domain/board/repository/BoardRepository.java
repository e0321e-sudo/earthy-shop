package com.earthy.shop.domain.board.repository;

import com.earthy.shop.domain.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 게시글 목록 조회
    Page<Board> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 게시글 제목, 내용 또는 작성자명 검색
    @Query("""
        select b
        from Board b
        join b.member m
        where b.title like concat('%', :keyword, '%')
           or b.content like concat('%', :keyword, '%')
           or m.name like concat('%', :keyword, '%')
        order by b.createdAt desc
        """)
    Page<Board> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
