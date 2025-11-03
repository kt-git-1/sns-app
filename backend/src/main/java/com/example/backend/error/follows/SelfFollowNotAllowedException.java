package com.example.backend.error.follows;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class SelfFollowNotAllowedException extends DomainException {
    public SelfFollowNotAllowedException() {
        super(DomainError.FOLLOW_SELF_NOT_ALLOWED, "follow.self_not_allowed");
    }
}
