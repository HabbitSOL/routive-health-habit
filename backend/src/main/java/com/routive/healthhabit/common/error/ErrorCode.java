package com.routive.healthhabit.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    REQUIRED_CONSENT_MISSING(HttpStatus.BAD_REQUEST, "필수 약관 동의가 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "갱신 토큰이 만료되었거나 유효하지 않습니다."),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "이메일 인증 토큰이 만료되었거나 유효하지 않습니다."),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "비밀번호 재설정 토큰이 만료되었거나 유효하지 않습니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "소셜 로그인 토큰이 유효하지 않습니다."),
    SOCIAL_PROVIDER_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 제공자 설정이 완료되지 않았습니다."),
    SOCIAL_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "소셜 인증 서버에 일시적으로 연결할 수 없습니다."),
    ACCOUNT_LINK_REQUIRED(HttpStatus.CONFLICT, "같은 이메일 계정이 존재합니다. 기존 로그인 방식을 사용해 주세요."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "이미 처리되었거나 중복된 요청입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DRUG_NOT_FOUND(HttpStatus.NOT_FOUND, "의약품을 찾을 수 없습니다."),
    MEDICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "개인 복약 정보를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
