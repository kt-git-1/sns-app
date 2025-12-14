package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PostRequest(
        @NotBlank(message = "{post.not_blank}")
        String content
) {}
