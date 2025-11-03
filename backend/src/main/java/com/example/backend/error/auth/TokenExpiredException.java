package com.example.backend.error.auth;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class TokenExpiredException extends DomainException {
    public TokenExpiredException() {
        super(DomainError.AUTH_TOKEN_EXPIRED, "auth.token_expired");
    }
}
