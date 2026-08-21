package com.earthy.shop.common.idempotency.scheduler;

import com.earthy.shop.common.idempotency.enums.IdempotencyStatus;
import com.earthy.shop.common.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyKeyCleanupScheduler {

    private static final int RETENTION_DAYS = 14;

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    // 오래된 완료/실패 멱등성 키 정리
    @Transactional
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupOldKeys() {
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deletedCount = idempotencyKeyRepository.deleteByStatusInAndCreatedAtBefore(
                List.of(IdempotencyStatus.COMPLETED, IdempotencyStatus.FAILED),
                expiredAt
        );

        if (deletedCount > 0) {
            log.info("[IDEMPOTENCY KEY CLEANUP] deletedCount={} | expiredAt={}",
                    deletedCount,
                    expiredAt);
        }
    }
}
