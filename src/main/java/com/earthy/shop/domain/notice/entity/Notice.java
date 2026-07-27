package com.earthy.shop.domain.notice.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제목
    @Column(nullable = false)
    private String title;

    // 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 고객 노출 여부
    @Column(nullable = false)
    private boolean visible = true;

    public Notice(String title, String content) {
        this.title = title;
        this.content = content;
        this.visible = true;
    }

    // 공지사항 수정
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // 공지사항 숨김 처리
    public void hide() {
        this.visible = false;
    }

    // 공지사항 다시 노출
    public void show() {
        this.visible = true;
    }
}