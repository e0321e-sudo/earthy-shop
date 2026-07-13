package com.earthy.shop.common.exception;

import com.earthy.shop.common.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(
            BusinessException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("[BUSINESS EXCEPTION] method={} | uri={} | code={} | message={}",
                request.getMethod(),
                request.getRequestURI(),
                errorCode.getCode(),
                errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.from(errorCode));
    }

    // 요청 값 검증 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        log.warn("[VALIDATION EXCEPTION] method={} | uri={} | message={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.from(errorCode));
    }

    // 요청 본문 파싱 예외 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        log.warn("[REQUEST BODY EXCEPTION] method={} | uri={} | message={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.from(errorCode));
    }

    // 예상하지 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("[UNEXPECTED EXCEPTION] method={} | uri={}",
                request.getMethod(),
                request.getRequestURI(),
                e);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.from(errorCode));
    }
}
