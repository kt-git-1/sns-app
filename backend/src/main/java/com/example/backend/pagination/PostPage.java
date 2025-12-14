package com.example.backend.pagination;

import java.util.List;

public record PostPage(
        List<PostSummaryDto> posts,
        String nextCursor,
        boolean hasNext
) {}
