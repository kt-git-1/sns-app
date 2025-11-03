package com.example.backend.error.follows;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class DuplicateFollowException extends DomainException {
    public DuplicateFollowException(String followerName, String followeeName) {
        super(DomainError.FOLLOW_ALREADY_EXISTS, "follow.already_exists", followerName, followeeName);
    }
}

