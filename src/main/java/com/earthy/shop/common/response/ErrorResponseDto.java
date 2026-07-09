package com.earthy.shop.common.response;

import com.earthy.shop.common.exception.ErrorCode;

public record ErrorResponseDto(
        boolean success,
        String code,
        String message
) {

    // 실패 응답 생성
    public static ErrorResponseDto from(ErrorCode errorCode) {
        return new ErrorResponseDto(
                false,
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }
}
