package com.example.backend.unit.service;

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
import com.example.backend.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // JUnit5でMockitoを使うための設定。これで@Mockが使える。
public class AuthServiceTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock HttpServletRequest httpReq;
    @Mock HttpServletResponse httpRes;

    AuthService sut; // system under test: テスト対象

    // 各テストの最初に呼ばれて、AuthServiceを初期化
    @BeforeEach
    void setUp() {
        sut = new AuthService(users, encoder, jwt);
    }

    // ========= signup =========
    // サインアップが成功した時にDBに保存される
    @Test
    void signup_ok_saves_user() {
        // given(準備)：モックの挙動を決める
        SignupRequest req = new SignupRequest("kaito2", "kaito2@example.com", "password123");
        when(users.existsByUsername("kaito2")).thenReturn(false);
        when(users.existsByEmail("kaito2@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("$2a$10$hash");

        // when(実行)：実際にテスト対象のメソッドを呼ぶ
        sut.signup(req);

        // then(検証)：結果や呼び出しを検証する
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("kaito2");
        assertThat(saved.getEmail()).isEqualTo("kaito2@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hash");
    }

    // username/emailが重複したらエラーを投げる
    @Test
    void signup_username_taken_throws() {
        // given
        SignupRequest req = new SignupRequest("kaito", "k@example.com", "password123");
        when(users.existsByUsername("kaito")).thenReturn(true);

        // when
        assertThatThrownBy(() -> sut.signup(req)).isInstanceOf(UsernameTakenException.class);

        // then
        verify(users, never()).save(any()); // DB保存は一度も呼ばれていないことを確認
    }

    @Test
    void signup_email_taken_throws() {
        // given
        SignupRequest req = new SignupRequest("kaito", "k@example.com", "password123");
        when(users.existsByUsername("kaito")).thenReturn(false);
        when(users.existsByEmail("k@example.com")).thenReturn(true);

        // when
        assertThatThrownBy(() -> sut.signup(req)).isInstanceOf(EmailTakenException.class);

        // then
        verify(users, never()).save(any()); // DB保存は一度も呼ばれていないことを確認
    }

    // ========= login =========
    // 正常なログインが行われているか確認
    @Test
    void login_ok_returns_access_and_sets_refresh_cookie() {
        // given
        var u = new User();
        u.setId(1L);
        u.setUsername("kaito");
        u.setEmail("kaito@example.com");
        u.setPasswordHash("$2a$hash");
        when(users.findByEmail("kaito@example.com")).thenReturn(Optional.of(u));
        when(encoder.matches("password123", "$2a$hash")).thenReturn(true);
        when(jwt.generateAccessToken(1L, "kaito")).thenReturn("access.jwt");
        when(jwt.generateRefreshToken(1L, "kaito")).thenReturn("refresh.jwt");

        // when
        TokenResponse res = sut.login(new LoginRequest("kaito@example.com", "password123"), httpRes);

        // then
        assertThat(res.accessToken()).isEqualTo("access.jwt");
        ArgumentCaptor<Cookie> cookieCap = ArgumentCaptor.forClass(Cookie.class);
        verify(httpRes).addCookie(cookieCap.capture());
        Cookie c = cookieCap.getValue();
        assertThat(c.getName()).isEqualTo("REFRESH_TOKEN");
        assertThat(c.getValue()).isEqualTo("refresh.jwt");
        assertThat(c.isHttpOnly()).isTrue();
        assertThat(c.getSecure()).isTrue();
        assertThat(c.getPath()).isEqualTo("/");
        assertThat(c.getMaxAge()).isPositive();
    }

    // email/passwordが見つからない→InvalidCredentialExceptionを投げるか確認
    @Test
    void login_email_not_found_throws() {
        // given
        when(users.findByEmail("kaito@example.com")).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> sut.login(new LoginRequest("kaito@example.com", "pw"), httpRes))
                .isInstanceOf(InvalidCredentialsException.class);

        // then
        verify(httpRes, never()).addCookie(any());
    }

    @Test
    void login_password_mismatch_throws() {
        //given
        var u = new User();
        u.setEmail("kaito@example.com");
        u.setPasswordHash("$2a$hash");
        when(users.findByEmail("kaito@example.com")).thenReturn(Optional.of(u));
        when(encoder.matches("bad", "$2a$hash")).thenReturn(false);

        // when
        assertThatThrownBy(() -> sut.login(new LoginRequest("kaito@example.com", "bad"), httpRes))
                .isInstanceOf(InvalidCredentialsException.class);

        // then
        verify(httpRes, never()).addCookie(any());
        verify(jwt, never()).generateAccessToken(any(), any());
    }

    // ========= refresh =========
    // refresh成功時に新しいaccessトークンが発行されるか確認
    @Test
    void refresh_ok_parses_cookie_and_issues_new_access() {
        // given(cookies準備)
        Cookie rc = new Cookie("REFRESH_TOKEN", "refresh.jwt");
        when(httpReq.getCookies()).thenReturn(new Cookie[]{ rc });
        // jwt.parseTokenの戻りをモック
        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getPayload()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("username")).thenReturn("kaito");
        when(jwt.parseToken("refresh.jwt")).thenReturn(jws);
        // user存在
        var u = new User();
        u.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(u));
        when(jwt.generateAccessToken(1L, "kaito")).thenReturn("new.access.jwt");

        // when
        TokenResponse res = sut.refresh(httpReq);

        // then
        assertThat(res.accessToken()).isEqualTo("new.access.jwt");
    }

    // Cookieがない場合は、TokenInvalidExceptionが投げられるか確認
    @Test
    void refresh_cookie_missing_throws() {
        when(httpReq.getCookies()).thenReturn(null);
        assertThatThrownBy(() -> sut.refresh(httpReq))
                .isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void refresh_user_missing_after_parse_throws() {
        Cookie rc = new Cookie("REFRESH_TOKEN", "refresh.jwt");
        when(httpReq.getCookies()).thenReturn(new Cookie[]{ rc });

        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getPayload()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("99");
        when(claims.get("username")).thenReturn("ghost");
        when(jwt.parseToken("refresh.jwt")).thenReturn(jws);

        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.refresh(httpReq))
                .isInstanceOf(TokenInvalidException.class);
    }

    // ========= logout =========
    //ログアウト成功時にCookieが削除されているか確認
    @Test
    void logout_ok_and_delete_cookie() {
        sut.logout(httpRes);
        ArgumentCaptor<Cookie> cookieCap = ArgumentCaptor.forClass(Cookie.class);
        verify(httpRes).addCookie(cookieCap.capture());
        Cookie c = cookieCap.getValue();
        assertThat(c.getName()).isEqualTo("REFRESH_TOKEN");
        assertThat(c.getMaxAge()).isZero();
        assertThat(c.isHttpOnly()).isTrue();
        assertThat(c.getSecure()).isTrue();
        assertThat(c.getPath()).isEqualTo("/");
    }
}
