package com.example.backend.pagination;

import java.time.OffsetDateTime;

public record Cursor(
    OffsetDateTime createdAt,
    Long id
) {}
