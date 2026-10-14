package com.routive.healthhabit.common.api;

import org.slf4j.MDC;

public record ApiResponse<T>(T data, String traceId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, MDC.get("traceId"));
    }
}
