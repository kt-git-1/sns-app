package com.example.backend.error;

import org.springframework.http.HttpStatus;

public enum DomainError {
    // 共通
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST),
    VALIDATION("VALIDATION", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT),
    INTERNAL("INTERNAL", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN),

    // 認証・認可
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED),
    AUTH_FORBIDDEN_ACTION("AUTH_FORBIDDEN_ACTION", HttpStatus.FORBIDDEN),

    // ユーザー
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", HttpStatus.CONFLICT),
    USERNAME_TAKEN("USERNAME_TAKEN", HttpStatus.CONFLICT),
    EMAIL_TAKEN("EMAIL_TAKEN", HttpStatus.CONFLICT),

    // フォロー
    FOLLOW_ALREADY_EXISTS("FOLLOW_ALREADY_EXISTS", HttpStatus.CONFLICT),
    FOLLOW_SELF_NOT_ALLOWED("FOLLOW_SELF_NOT_ALLOWED", HttpStatus.UNPROCESSABLE_ENTITY),

    // 投稿
    POST_TOO_LONG("POST_TOO_LONG", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String code;
    private final HttpStatus status;

    DomainError(String code, HttpStatus status) {
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
}