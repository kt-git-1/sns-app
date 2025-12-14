package com.example.backend.pagination;

import java.time.OffsetDateTime;

public record PostSummaryDto(
        Long id,
        Long userId,
        String username,
        String content,
        OffsetDateTime createdAt
) {
}
