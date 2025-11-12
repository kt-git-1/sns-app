package com.example.backend.security;

import com.example.backend.error.auth.TokenExpiredException;
import com.example.backend.error.auth.TokenInvalidException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.time.Clock;

/**
 * JWTの発行・検証ユーティリティ。
 * - HS256（対称鍵）で署名
 * - Access/Refresh の発行
 * - 署名とexpを検証し、Claimsを取り出す
 */
@Component
public class JwtService {

    private final String secretString;
    private final long accessExpMinutes;
    private final long refreshExpDays;
    private final String issuer;
    private final Clock clock;

    private SecretKey secretKey;
    private JwtParser parser;

    public JwtService(
            @Value("${spring.jwt.secret}") String secretString,
            @Value("${spring.jwt.access-exp-minutes}") long accessExpMinutes,
            @Value("${spring.jwt.refresh-exp-days}") long refreshExpDays,
            @Value("${spring.jwt.issuer}") String issuer,
            Clock clock
    ) {
        this.secretString = secretString;
        this.accessExpMinutes = accessExpMinutes;
        this.refreshExpDays = refreshExpDays;
        this.issuer = issuer;
        this.clock = clock;
    }

    @PostConstruct
    public void init() {
        // 例：Base64で渡す場合は Base64.getDecoder().decode(secretString)
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));

        this.parser = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(secretKey)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    private String buildToken(Long userId, String username, Instant now, Instant exp, Map<String, Object> extra) {
        JwtBuilder b = Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("token_type", extra.getOrDefault("token_type", "access")) // ← 衝突を避ける命名
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey, Jwts.SIG.HS256);

        extra.forEach((k, v) -> { if (!"token_type".equals(k)) b.claim(k, v); });
        return b.compact();
    }

    public String generateAccessToken(Long userId, String username) {
        Instant now = clock.instant();
        Instant exp = now.plus(accessExpMinutes, ChronoUnit.MINUTES);
        return buildToken(userId, username, now, exp, Map.of("token_type", "access"));
    }

    public String generateRefreshToken(Long userId, String username) {
        Instant now = clock.instant();
        Instant exp = now.plus(refreshExpDays, ChronoUnit.DAYS);
        return buildToken(userId, username, now, exp, Map.of("token_type", "refresh"));
    }

    public Jws<Claims> parseToken(String token) {
        try {
            return parser.parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException e) {
            throw new TokenInvalidException();
        }
    }

    public boolean isExpired(String token) {
        try {
            Date exp = parseToken(token).getPayload().getExpiration();
            return exp.before(Date.from(clock.instant()));
        } catch (TokenInvalidException | TokenExpiredException e) {
            return true; // 署名不正/期限切れ含め利用不可とみなす
        }
    }

    public String getTokenType(String token) {
        try {
            Object t = parseToken(token).getPayload().get("token_type");
            return t == null ? null : t.toString();
        } catch (RuntimeException e) { // ラップ後の例外
            return null;
        }
    }

    /** API側で明確にチェックできる補助 */
    public void validateAccessToken(String token) {
        var claims = parseToken(token).getPayload();
        if (!"access".equals(claims.get("token_type"))) {
            throw new TokenInvalidException();
        }
    }

    public void validateRefreshToken(String token) {
        var claims = parseToken(token).getPayload();
        if (!"refresh".equals(claims.get("token_type"))) {
            throw new TokenInvalidException();
        }
    }
}