package com.earthy.shop.common.idempotency.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    // 멱등성 키 조회
    public IdempotencyKey find(
            String memberEmail,
            String idempotencyKey,
            String apiPath
    ) {
        return idempotencyKeyRepository
                .findByMemberEmailAndIdempotencyKeyAndApiPathForUpdate(memberEmail, idempotencyKey, apiPath)
                .orElse(null);
    }

    // 멱등성 키 생성
    @Transactional
    public IdempotencyKey create(
            String memberEmail,
            String idempotencyKey,
            String apiPath
    ) {
        try {
            return idempotencyKeyRepository.saveAndFlush(
                    new IdempotencyKey(memberEmail, idempotencyKey, apiPath)
            );
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
        }
    }

    // 처리 완료
    @Transactional
    public void complete(
            IdempotencyKey idempotencyKey,
            Long resourceId,
            String responseMessage
    ) {
        idempotencyKey.complete(resourceId, responseMessage);
    }

    // 처리 실패
    @Transactional
    public void fail(
            IdempotencyKey idempotencyKey,
            String responseMessage
    ) {
        idempotencyKey.fail(responseMessage);
    }
}
