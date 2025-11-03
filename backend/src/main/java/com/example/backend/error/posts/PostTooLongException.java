package com.example.backend.error.posts;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class PostTooLongException extends DomainException {
    public PostTooLongException(int limit, int actual) {
        super(DomainError.POST_TOO_LONG, "post.too_long", limit, actual);
    }
}
