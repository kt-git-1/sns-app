package com.example.backend.validation;

import com.example.backend.controller.AuthController;
import com.example.backend.error.GlobalExceptionHandler;
import com.example.backend.error.ProblemBuilder;
import com.example.backend.security.JwtAuthFilter;
import com.example.backend.security.JwtService;
import com.example.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerValidationTest {

    @Autowired MockMvc mvc;
    @MockitoBean AuthService authService;
    @MockitoBean ProblemBuilder problem;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void stubProblemBuilder() { // ステータス付きの空ProblemDetailを返し、errors プロパティを実際に付与する
        // どのstatusでも素のProblemDetailを返す
        when(problem.buildFromKeys(any(HttpStatus.class), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    HttpStatus st = inv.getArgument(0);
                    return ProblemDetail.forStatus(st);
                });

        // withProperty(pd, name, value)が呼ばれたら、pd.setProperty(name, value)する
        doAnswer(inv -> {
            ProblemDetail pd = inv.getArgument(0);
            String name = inv.getArgument(1);
            Object value =inv.getArgument(2);
            pd.setProperty(name, value);
            return null;
        }).when(problem).withProperty(any(ProblemDetail.class), anyString(), any());
    }

    // ====== サインアップ：空白 ======
    @Test
    void signup_blank_all_returns_400_with_errors_array() throws Exception {
        mvc.perform(post("/auth/signup")
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"username":"","email":"","password":""}
                    """))
           .andExpect(status().isBadRequest())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.errors").isArray())
           .andExpect(jsonPath("$.errors[*].field").value(hasItems("username","email","password")));

        // Validationで弾かれたので、Serviceは呼ばれない
        verifyNoInteractions(authService);
    }

    // ====== サインアップ：メール形式不正 ======
    @Test
    void signup_invalid_email_returns_400_and_email_in_errors() throws Exception {
        mvc.perform(post("/auth/signup")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"username":"kaito","email":"not-an-email","password":"passw0rd"}
                """))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errors").isArray())
           .andExpect(jsonPath("$.errors[?(@.field=='email')]").isNotEmpty());
        verifyNoInteractions(authService);
    }

    // ====== サインアップ：サイズ ======
    // username: min=3, max=50
    @ParameterizedTest
    @MethodSource("invalidUsernames")
    void signup_username_size_violation_returns_400(String username) throws Exception {
        String body = """
                {"username":"%s","email":"k@example.com","password":"passw0rd"}
                """.formatted(username);
        mvc.perform(post("/auth/signup")
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                .content(body))
           .andExpect(status().isBadRequest())
           .andExpect(content().contentType(APPLICATION_JSON))
           .andExpect(jsonPath("$.errors").isArray())
           .andExpect(jsonPath("$.errors[?(@.field=='username')]").isNotEmpty())
           .andExpect(jsonPath("$.errors[?(@.field=='username')].code").value(hasItem("Size")));
        verifyNoInteractions(authService);
    }

    // email: max=100
    @ParameterizedTest
    @MethodSource("invalidEmail")
    void signup_email_size_violation_returns_400(String email) throws Exception {
        String body = """
                {"username":"kaito","email":"%s","password":"passw0rd"}
                """.formatted(email);
        mvc.perform(post("/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='email')].code").value(hasItem("Size")));
        verifyNoInteractions(authService);
    }

    // password: min=8, max=72
    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void signup_password_size_violation_returns_400(String password) throws Exception {
        String body = """
                {"username":"kaito","email":"k@example.com","password":"%s"}
                """.formatted(password);
        mvc.perform(post("/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].code").value(hasItem("Size")));
        verifyNoInteractions(authService);
    }

    // ====== ログイン：空白 ======
    @Test
    void login_blank_returns_400_with_errors() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"email":"","password":""}
                """))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errors").isArray())
           .andExpect(jsonPath("$.errors[*].field").value(hasItems("email","password")));
        verifyNoInteractions(authService);
    }

    // ====== ログイン：サイズ ======
    // email: max=100
    @ParameterizedTest
    @MethodSource("invalidEmail")
    void login_email_size_violation_returns_400(String email) throws Exception {
        String body = """
                {"username":"kaito","email":"%s","password":"passw0rd"}
                """.formatted(email);
        mvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='email')].code").value(hasItem("Size")));
        verifyNoInteractions(authService);
    }

    // password: min=8, max=72
    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void login_password_size_violation_returns_400(String password) throws Exception {
        String body = """
                {"username":"kaito","email":"k@example.com","password":"%s"}
                """.formatted(password);
        mvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].code").value(hasItem("Size")));
        verifyNoInteractions(authService);
    }

    // ====== バリデーションメソッド ======
    static Stream<String> invalidUsernames() {
        return Stream.of(
                "aa", // min-1
                "a".repeat(51) // max+1
        );
    }

    static Stream<String> invalidEmail() {
        return Stream.of(
                "a".repeat(89) + "@example.com" // max+1(101)
        );
    }

    static Stream<String> invalidPasswords() {
        return Stream.of(
                "aaaaaaa",
                "a".repeat(73)
        );
    }
}
