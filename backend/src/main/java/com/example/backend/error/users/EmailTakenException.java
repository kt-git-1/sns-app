package com.example.backend.error.users;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class EmailTakenException extends DomainException {
    public EmailTakenException(String email) {
        super(DomainError.EMAIL_TAKEN, "user.email_taken", email);
    }
}
