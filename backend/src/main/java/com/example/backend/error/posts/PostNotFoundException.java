package com.example.backend.error.posts;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class PostNotFoundException extends DomainException {
    public PostNotFoundException() {
        super(DomainError.POST_NOT_FOUND, "post.not_found");
    }
}
