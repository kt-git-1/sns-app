package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * サインアップ入力DTO。
 * Controllerで @Valid を付けて受け取り、Serviceにそのまま渡す。
 */
public record SignupRequest(
        @NotBlank(message = "{username.not_blank}")
        @Size(min = 3, max = 50, message = "{username.size}")
        String username,

        @NotBlank(message = "{email.not_blank}")
        @Email(message = "{email.format}")
        @Size(max = 100, message = "{email.size}")
        String email,

        @NotBlank(message = "{password.not_blank}")
        @Size(min = 8, max = 72, message = "{password.size}")
        String password
) {}
