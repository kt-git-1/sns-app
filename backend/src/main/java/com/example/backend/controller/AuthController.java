package com.example.backend.controller;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.SignupRequest;
import com.example.backend.dto.TokenResponse;
import com.example.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    // ===== サインアップ =====
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest req) {
        // サインアップ処理
        auth.signup(req);

        // 201 CREATED
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ===== ログイン =====
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        // ログイン処理(Accessトークンをreturn + RefreshトークンをCookieにセット)
        return auth.login(req, res);
    }

    // ===== Accessトークン再発行 =====
    @PostMapping("/refresh")
    public TokenResponse refresh(HttpServletRequest req) {
        // Refresh処理(Refreshトークン検証 + Accessトークン再発行)
        return auth.refresh(req);
    }

    // ===== ログアウト =====
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse res) {
        // ログアウト処理(Cookieを空にする)
        auth.logout(res);

        // 204 No Content
        return ResponseEntity.noContent().build();
    }
}
