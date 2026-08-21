package com.earthy.shop.common.idempotency.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.enums.IdempotencyStatus;
import com.earthy.shop.common.idempotency.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    void 멱등성_키를_잠금_조회한다() {
        // given
        IdempotencyKey idempotencyKey = idempotencyKey();

        given(idempotencyKeyRepository.findByMemberEmailAndIdempotencyKeyAndApiPathForUpdate(
                "test@example.com",
                "request-key",
                "/api/orders"
        )).willReturn(Optional.of(idempotencyKey));

        // when
        IdempotencyKey foundKey = idempotencyService.find(
                "test@example.com",
                "request-key",
                "/api/orders"
        );

        // then
        assertThat(foundKey).isSameAs(idempotencyKey);
        verify(idempotencyKeyRepository).findByMemberEmailAndIdempotencyKeyAndApiPathForUpdate(
                "test@example.com",
                "request-key",
                "/api/orders"
        );
    }

    @Test
    void 멱등성_키가_없으면_null을_반환한다() {
        // given
        given(idempotencyKeyRepository.findByMemberEmailAndIdempotencyKeyAndApiPathForUpdate(
                "test@example.com",
                "request-key",
                "/api/orders"
        )).willReturn(Optional.empty());

        // when
        IdempotencyKey foundKey = idempotencyService.find(
                "test@example.com",
                "request-key",
                "/api/orders"
        );

        // then
        assertThat(foundKey).isNull();
    }

    @Test
    void 멱등성_키를_생성한다() {
        // given
        given(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        IdempotencyKey savedKey = idempotencyService.create(
                "test@example.com",
                "request-key",
                "/api/orders"
        );

        // then
        assertThat(savedKey.getMemberEmail()).isEqualTo("test@example.com");
        assertThat(savedKey.getIdempotencyKey()).isEqualTo("request-key");
        assertThat(savedKey.getApiPath()).isEqualTo("/api/orders");
        assertThat(savedKey.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
    }

    @Test
    void 멱등성_키_생성_중_중복_충돌이면_처리중_예외가_발생한다() {
        // given
        given(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                .willThrow(new DataIntegrityViolationException("duplicate idempotency key"));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> idempotencyService.create(
                        "test@example.com",
                        "request-key",
                        "/api/orders"
                ))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING)
                );
    }

    @Test
    void 멱등성_키를_처리_완료로_변경한다() {
        // given
        IdempotencyKey idempotencyKey = idempotencyKey();

        // when
        idempotencyService.complete(idempotencyKey, 1L, "주문 생성 성공");

        // then
        assertThat(idempotencyKey.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(idempotencyKey.getResourceId()).isEqualTo(1L);
        assertThat(idempotencyKey.getResponseMessage()).isEqualTo("주문 생성 성공");
        assertThat(idempotencyKey.isCompleted()).isTrue();
    }

    @Test
    void 멱등성_키를_처리_실패로_변경한다() {
        // given
        IdempotencyKey idempotencyKey = idempotencyKey();

        // when
        idempotencyService.fail(idempotencyKey, "주문 생성 실패");

        // then
        assertThat(idempotencyKey.getStatus()).isEqualTo(IdempotencyStatus.FAILED);
        assertThat(idempotencyKey.getResponseMessage()).isEqualTo("주문 생성 실패");
        assertThat(idempotencyKey.isProcessing()).isFalse();
    }

    private IdempotencyKey idempotencyKey() {
        return new IdempotencyKey(
                "test@example.com",
                "request-key",
                "/api/orders"
        );
    }
}
