package com.example.backend.dto;

import java.time.OffsetDateTime;

public record PostResponse(
        Long id,
        String content,
        String username,
        OffsetDateTime createdAt
) {}
