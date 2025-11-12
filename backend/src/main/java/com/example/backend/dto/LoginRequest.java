package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "{email.not_blank}")
        @Size(max = 100, message = "{email.size}")
        String email,

        @NotBlank(message = "{password.not_blank}")
        @Size(min = 8, max = 72, message = "{password.size}")
        String password
) {}
