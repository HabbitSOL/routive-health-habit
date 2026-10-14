package com.routive.healthhabit.common.error;

import java.util.List;
import org.slf4j.MDC;

public record ErrorResponse(ErrorBody error, String traceId) {

    public static ErrorResponse of(ErrorCode code) {
        return of(code, code.message(), List.of());
    }

    public static ErrorResponse of(ErrorCode code, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                new ErrorBody(code.name(), message, fieldErrors),
                MDC.get("traceId")
        );
    }

    public record ErrorBody(String code, String message, List<FieldError> fieldErrors) {
    }

    public record FieldError(String field, String reason) {
    }
}
