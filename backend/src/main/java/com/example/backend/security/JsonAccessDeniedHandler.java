package com.example.backend.security;

import com.example.backend.error.ProblemBuilder;
import jakarta.servlet.http.*;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final MessageSource ms;
    private final ProblemBuilder problem;

    public JsonAccessDeniedHandler(MessageSource ms, ProblemBuilder problem) {
        this.ms = ms;
        this.problem = problem;
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        var locale = LocaleContextHolder.getLocale();
        var detail = ms.getMessage("error.forbidden", null, locale);

        ProblemDetail pd = problem.build(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                detail,
                "FORBIDDEN",
                "forbidden"
        );
        problem.write(res, pd, HttpStatus.FORBIDDEN);
    }
}