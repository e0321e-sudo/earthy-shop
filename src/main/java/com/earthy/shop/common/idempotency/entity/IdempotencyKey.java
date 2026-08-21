package com.earthy.shop.common.idempotency.entity;

import com.earthy.shop.common.entity.BaseTimeEntity;
import com.earthy.shop.common.idempotency.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_member_key_api",
                        columnNames = {"member_email", "idempotency_key", "api_path"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청 회원 이메일
    @Column(name = "member_email", nullable = false)
    private String memberEmail;

    // 클라이언트가 전달한 멱등성 키
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    // 멱등성을 적용할 API 경로
    @Column(name = "api_path", nullable = false)
    private String apiPath;

    // 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    // 최초 요청 결과 식별자
    @Column(name = "resource_id")
    private Long resourceId;

    // 최초 요청 결과 메시지
    @Column(name = "response_message")
    private String responseMessage;

    public IdempotencyKey(
            String memberEmail,
            String idempotencyKey,
            String apiPath
    ) {
        this.memberEmail = memberEmail;
        this.idempotencyKey = idempotencyKey;
        this.apiPath = apiPath;
        this.status = IdempotencyStatus.PROCESSING;
    }

    // 처리 완료
    public void complete(Long resourceId, String responseMessage) {
        this.resourceId = resourceId;
        this.responseMessage = responseMessage;
        this.status = IdempotencyStatus.COMPLETED;
    }

    // 처리 실패
    public void fail(String responseMessage) {
        this.responseMessage = responseMessage;
        this.status = IdempotencyStatus.FAILED;
    }

    // 처리 완료 여부
    public boolean isCompleted() {
        return this.status == IdempotencyStatus.COMPLETED;
    }

    // 처리 중 여부
    public boolean isProcessing() {
        return this.status == IdempotencyStatus.PROCESSING;
    }
}