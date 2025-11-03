package com.example.backend.error.users;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(Long id) {
        super(DomainError.USER_NOT_FOUND, "user.not_found", id);
    }
}

