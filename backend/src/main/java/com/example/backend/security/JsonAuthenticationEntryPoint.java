package com.example.backend.security;

import com.example.backend.error.ProblemBuilder;
import jakarta.servlet.http.*;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MessageSource ms;
    private final ProblemBuilder problem;

    public JsonAuthenticationEntryPoint(MessageSource ms, ProblemBuilder problem) {
        this.ms = ms;
        this.problem = problem;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        var locale = LocaleContextHolder.getLocale();
        var detail = ms.getMessage("error.unauthorized", null, locale);

        ProblemDetail pd = problem.build(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                detail,
                "UNAUTHORIZED",
                "unauthorized"
        );
        problem.write(res, pd, HttpStatus.UNAUTHORIZED);
    }
}