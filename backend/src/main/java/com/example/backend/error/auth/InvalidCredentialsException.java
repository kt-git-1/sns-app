package com.example.backend.error.auth;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super(DomainError.AUTH_INVALID_CREDENTIALS, "auth.invalid_credentials");
    }
}