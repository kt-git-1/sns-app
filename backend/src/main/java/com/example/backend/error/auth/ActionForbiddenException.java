package com.example.backend.error.auth;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class ActionForbiddenException extends DomainException {
    public ActionForbiddenException() {
        super(DomainError.AUTH_FORBIDDEN_ACTION, "auth.action_forbidden");
    }
}
