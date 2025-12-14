package com.example.backend.error.posts;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class PostActionForbiddenException extends DomainException {
    public PostActionForbiddenException() {
        super(DomainError.POST_FORBIDDEN_ACTION, "post.action_forbidden");
    }
}
