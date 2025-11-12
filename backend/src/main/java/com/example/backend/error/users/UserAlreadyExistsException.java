package com.example.backend.error.users;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String username) {
        super(DomainError.USER_ALREADY_EXISTS, "user.already_exists", username);
    }
}
