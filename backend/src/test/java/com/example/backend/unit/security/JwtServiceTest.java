package com.example.backend.unit.security;

import com.example.backend.error.auth.TokenExpiredException;
import com.example.backend.error.auth.TokenInvalidException;
import com.example.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private static final String ISSUER = "test-issuer";
    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static final long ACCESS_EXP_MIN = 15L;
    private static final long REFRESH_EXP_DAYS = 7L;

    private Instant t0;
    private Clock clockT0;
    private JwtService serviceAtT0;

    @BeforeEach
    void setUp() {
        t0 = Instant.parse("2025-01-01T00:00:00Z");
        clockT0 = Clock.fixed(t0, ZoneOffset.UTC);
        serviceAtT0 = new JwtService(
                SECRET,
                ACCESS_EXP_MIN,
                REFRESH_EXP_DAYS,
                ISSUER,
                clockT0
        );
        serviceAtT0.init();
    }

    // Goal: A valid access token has the right claims and times.
    @Test
    void generateAccessToken_containsExpectedClaimAndExp() {
        String token = serviceAtT0.generateAccessToken(123L, "alice");
        Jws<Claims> jws = serviceAtT0.parseToken(token);
        Claims c = jws.getPayload();

        assertEquals(ISSUER, c.getIssuer());
        assertEquals("123", c.getSubject());
        assertEquals("alice", c.get("username"));
        assertEquals("access", c.get("token_type"));

        assertEquals(Date.from(t0), c.getIssuedAt()); // ClockのインスタンスとしてclockT0を使っているから必ずt0にfixされる
        assertEquals(Date.from(t0.plus(ACCESS_EXP_MIN, ChronoUnit.MINUTES)), c.getExpiration());
    }

    // Goal: Refresh tokens are similar but last longer.
    @Test
    void generateRefreshToken_containsExpectedClaimsAndLongerExp() {
        String token = serviceAtT0.generateRefreshToken(1L, "kaito");
        Jws<Claims> jws = serviceAtT0.parseToken(token);
        Claims c = jws.getPayload();

        assertEquals("1", c.getSubject());
        assertEquals("kaito", c.get("username"));
        assertEquals("refresh", c.get("token_type"));
        assertEquals(Date.from(t0.plus(REFRESH_EXP_DAYS, ChronoUnit.DAYS)), c.getExpiration());
    }

    // Goal: Parsing a valid token returns claims without exceptions.
    @Test
    void parseToken_returnsClaimsForValidToken() {
        String token = serviceAtT0.generateAccessToken(2L, "cyou");
        Jws<Claims> jws = serviceAtT0.parseToken(token);

        assertNotNull(jws.getPayload());
        assertEquals("cyou", jws.getPayload().get("username"));
    }

    // Goal: Expired tokens are rejected with the right exception.
    @Test
    void parseToken_withExpiredToken_throwsTokenExpiredException() {
        JwtService shortExpService = new JwtService(
                SECRET,
                1,
                REFRESH_EXP_DAYS,
                ISSUER,
                clockT0
        );
        shortExpService.init();
        String token = shortExpService.generateAccessToken(3L, "k");

        Clock clockT0Plus2 = Clock.fixed(t0.plus(2, ChronoUnit.MINUTES), ZoneOffset.UTC);
        JwtService parserAfterExp = new JwtService(
                SECRET,
                ACCESS_EXP_MIN,
                REFRESH_EXP_DAYS,
                ISSUER,
                clockT0Plus2
        );
        parserAfterExp.init();

        assertThrows(TokenExpiredException.class, () -> parserAfterExp.parseToken(token));
    }

    // Goal: Tampered tokens (bad signature) are rejected.
    @Test
    void parseToken_withInvalidSignature_throwsTokenInvalidException() {
        String valid = serviceAtT0.generateAccessToken(1L, "u");
        String tampered = valid.substring(0, valid.length() - 2) + "xx";

        assertThrows(TokenInvalidException.class, () -> serviceAtT0.parseToken(tampered));
    }

    // Goal: Tokens from other issuers are rejected.
    @Test
    void parseToken_withWrongIssuer_throwsTokenInvalidException() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String wrongIssuerToken = Jwts.builder()
                .issuer("another-issuer")
                .subject("77")
                .claim("username", "aaa")
                .claim("token_type", "access")
                .issuedAt(Date.from(t0))
                .expiration(Date.from(t0.plus(ACCESS_EXP_MIN, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThrows(TokenInvalidException.class, () -> serviceAtT0.parseToken(wrongIssuerToken));
    }
}
