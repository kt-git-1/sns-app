package com.example.backend.error.auth;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class TokenInvalidException extends DomainException {
    public TokenInvalidException() {
        super(DomainError.AUTH_TOKEN_INVALID, "auth.token_invalid");
    }
}
