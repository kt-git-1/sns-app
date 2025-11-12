package com.example.backend.dto;

/**
 * アクセストークン返却用DTO。
 * Refresh TokenはHttpOnly Cookieで返す想定のため、ここには含めない。
 */
public record TokenResponse(
        String accessToken
) {}
