package com.earthy.shop.common.response;

public record ApiResponseDto<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    // 성공 응답
    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>(
                true,
                "SUCCESS",
                "요청 성공",
                data
        );
    }

    // 성공 응답 메시지 지정
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(
                true,
                "SUCCESS",
                message,
                data
        );
    }
}