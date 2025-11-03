package com.example.backend.error.users;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class UsernameTakenException extends DomainException {
    public UsernameTakenException(String username) {
        super(DomainError.USERNAME_TAKEN, "user.username_taken", username);
    }
}

