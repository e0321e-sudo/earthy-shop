package com.earthy.shop.domain.board.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.domain.board.enums.BoardStatus;
import com.earthy.shop.domain.board.enums.BoardType;
import com.earthy.shop.domain.board.enums.BoardVisibility;
import com.earthy.shop.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "boards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 문의 종류
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardType type;

    // 게시글 제목
    @Column(nullable = false)
    private String title;

    // 게시글 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 공개 여부
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardVisibility visibility;

    // 비공개 게시글 비밀번호
    private String postPassword;

    // 관리자 답변
    @Column(columnDefinition = "TEXT")
    private String answer;

    // 답변 등록 시간
    private LocalDateTime answeredAt;

    // 답변 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardStatus status = BoardStatus.WAITING;

    public Board(
            Member member,
            BoardType type,
            String title,
            String content,
            BoardVisibility visibility,
            String postPassword
    ) {
        this.member = member;
        this.type = type;
        this.title = title;
        this.content = content;
        this.visibility = visibility;
        this.postPassword = postPassword;
        this.status = BoardStatus.WAITING;
    }

    // 게시글 수정
    public void update(BoardType type, String title, String content, BoardVisibility visibility, String postPassword) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.visibility = visibility;
        this.postPassword = postPassword;
    }

    // 관리자 답변 등록
    public void answer(String answer) {
        this.answer = answer;
        this.status = BoardStatus.ANSWERED;
        this.answeredAt = LocalDateTime.now();
    }

    // 게시글 수정 가능 여부
    public boolean canUpdate() {
        return this.status == BoardStatus.WAITING;
    }

    // 비공개 게시글 여부
    public boolean isPrivate() {
        return this.visibility == BoardVisibility.PRIVATE;
    }

    // 작성자 본인 여부
    public boolean isWrittenBy(Member member) {
        return this.member.getId().equals(member.getId());
    }

}
