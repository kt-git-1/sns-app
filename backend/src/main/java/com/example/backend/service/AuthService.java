package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.SignupRequest;
import com.example.backend.dto.TokenResponse;
import com.example.backend.entity.User;
import com.example.backend.error.auth.InvalidCredentialsException;
import com.example.backend.error.auth.TokenInvalidException;
import com.example.backend.error.users.EmailTakenException;
import com.example.backend.error.users.UsernameTakenException;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

@Service
public class AuthService {

    private static final String REFRESH_COOKIE = "REFRESH_TOKEN";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // ===== サインアップ =====
    @Transactional
    public void signup(SignupRequest req) {
        // 重複チェック(ユーザー名、メアド)
        if (users.existsByUsername(req.username())) {
            throw new UsernameTakenException(req.username());
        }
        if (users.existsByEmail(req.email())) {
            throw new EmailTakenException(req.email());
        }

        // パスワードハッシュ化
        String hash = encoder.encode(req.password());

        // DB保存
        User u = new User();
        u.setUsername(req.username());
        u.setEmail(req.email());
        u.setPasswordHash(hash);
        users.save(u);
    }

    // ===== ログイン =====
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req, HttpServletResponse res) {
        // ユーザーの存在チェック
        var u = users.findByEmail(req.email())
                .orElseThrow(InvalidCredentialsException::new);

        // パスワードチェック
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        //JWT発行
        String access = jwt.generateAccessToken(u.getId(), u.getUsername());
        String refresh = jwt.generateRefreshToken(u.getId(), u.getUsername());

        // HttpOnly CookieにRefreshトークンを付与
        addRefreshCookie(res, refresh);

        return new TokenResponse(access);
    }

    // ===== Accessトークンをリフレッシュ =====
    @Transactional(readOnly = true)
    public TokenResponse refresh(HttpServletRequest req) {
        // Refreshトークン取得
        String refresh = readRefreshCookie(req)
                .orElseThrow(TokenInvalidException::new);

        // 署名/期限を検証 + クレーム取り出し
        var claims = jwt.parseToken(refresh).getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        String username = (String) claims.get("username");

        // ユーザーが存在するか確認
        users.findById(userId).orElseThrow(TokenInvalidException::new);

        // 新しいAccessトークンを発行
        String newAccess = jwt.generateAccessToken(userId, username);

        return new TokenResponse(newAccess);
    }

    // ===== ログアウト =====
    @Transactional
    public void logout(HttpServletResponse res) {
        // Cookieを空にする
        clearRefreshCookie(res);
    }

    // ===== Cookieユーティリティ =====
    // CookieにRefreshトークンを追加
    private void addRefreshCookie(HttpServletResponse res, String value) {
        // RefreshトークンをCookieにセット
        Cookie c = new Cookie(REFRESH_COOKIE, value);
        c.setHttpOnly(true);
        c.setSecure(true);
        c.setPath("/");
        c.setMaxAge(60 * 60 * 24 * 14);
        res.addCookie(c);
    }

    // CookieのRefreshトークンを読み取る
    private Optional<String> readRefreshCookie(HttpServletRequest req) {
        // Cookieが空の場合の処理
        if (req.getCookies() == null) return Optional.empty();

        // Refreshトークンを取り出す
        return Arrays.stream(req.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    // Cookieを空にする
    private void clearRefreshCookie(HttpServletResponse res) {
        // 値が空のCookieをセット
        Cookie c = new Cookie(REFRESH_COOKIE, "");
        c.setHttpOnly(true);
        c.setSecure(true);
        c.setPath("/");
        c.setMaxAge(0);
        res.addCookie(c);
    }

}
