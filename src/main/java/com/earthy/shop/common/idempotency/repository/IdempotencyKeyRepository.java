package com.earthy.shop.common.idempotency.repository;

import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.enums.IdempotencyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    // 멱등성 키 단건 조회
    Optional<IdempotencyKey> findByMemberEmailAndIdempotencyKeyAndApiPath(
            String memberEmail,
            String idempotencyKey,
            String apiPath
    );

    // 멱등성 키 단건 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from IdempotencyKey i
            where i.memberEmail = :memberEmail
              and i.idempotencyKey = :idempotencyKey
              and i.apiPath = :apiPath
            """)
    Optional<IdempotencyKey> findByMemberEmailAndIdempotencyKeyAndApiPathForUpdate(
            @Param("memberEmail") String memberEmail,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("apiPath") String apiPath
    );

    // 멱등성 키 존재 여부 확인
    boolean existsByMemberEmailAndIdempotencyKeyAndApiPath(
            String memberEmail,
            String idempotencyKey,
            String apiPath
    );

    // 오래된 완료/실패 멱등성 키 삭제
    @Modifying
    @Query("""
            delete from IdempotencyKey i
            where i.status in :statuses
              and i.createdAt < :expiredAt
            """)
    int deleteByStatusInAndCreatedAtBefore(
            @Param("statuses") Collection<IdempotencyStatus> statuses,
            @Param("expiredAt") LocalDateTime expiredAt
    );
}
