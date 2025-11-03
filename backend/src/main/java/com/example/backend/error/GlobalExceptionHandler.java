package com.example.backend.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ProblemBuilder problem;

    public GlobalExceptionHandler(ProblemBuilder problem) {
        this.problem = problem;
    }

    /* =========================
     * ドメイン例外
     * ========================= */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        var err = ex.getError();
        var pd = problem.buildFromKeys(
                err.status(),
                null, null,              // titleKeyは省略→HTTPの既定文言
                ex.getMessageKey(), ex.getMessageArgs(), // detailはmessages.propertiesから
                err.code(),                              // code
                err.code().toLowerCase()                 // type suffix
        );
        return ResponseEntity.status(err.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /* =========================
     * バリデーション系
     * ========================= */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldMap)
                .toList();

        var pd = problem.buildFromKeys(
                HttpStatus.BAD_REQUEST,
                null, null,
                "error.validation", null,
                "VALIDATION",
                "validation"
        );
        problem.withProperty(pd, "errors", fields);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        var fields = ex.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "field", v.getPropertyPath().toString(),
                        "code", v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        "message", v.getMessage()
                ))
                .toList();

        var pd = problem.buildFromKeys(
                HttpStatus.BAD_REQUEST,
                null, null,
                "error.validation", null,
                "VALIDATION",
                "validation"
        );
        problem.withProperty(pd, "errors", fields);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /* =========================
     * 最後の砦
     * ========================= */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleOthers(Exception ex) {
        ex.printStackTrace(); // ← ログに残して原因調査できるようにする

        var pd = problem.buildFromKeys(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "error.internal", null,    // タイトル
                "error.internal", null,    // 詳細
                "INTERNAL",                // コード
                "internal-error"           // type suffix
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private Map<String, Object> toFieldMap(FieldError fe) {
        return Map.of(
                "field", fe.getField(),
                "code", fe.getCode(),
                "message", fe.getDefaultMessage()
        );
    }
}
