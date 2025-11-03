package com.example.backend.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;

@Component
public class ProblemBuilder {

    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public ProblemBuilder(MessageSource messageSource, ObjectMapper objectMapper) {
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    /** 文字列（既に解決済み）から組み立て */
    public ProblemDetail build(HttpStatus status,
                               String title,
                               String detail,
                               @Nullable String code,
                               @Nullable String typeSuffixOrUri) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("timestamp", OffsetDateTime.now());
        if (code != null) pd.setProperty("code", code);
        pd.setType(resolveType(code, typeSuffixOrUri));
        return pd;
    }

    /** メッセージキーから組み立て（messages.propertiesを解決） */
    public ProblemDetail buildFromKeys(HttpStatus status,
                                       @Nullable String titleKey, @Nullable Object[] titleArgs,
                                       String detailKey, @Nullable Object[] detailArgs,
                                       @Nullable String code,
                                       @Nullable String typeSuffixOrUri) {
        var locale = LocaleContextHolder.getLocale();
        String title = (titleKey == null)
                ? status.getReasonPhrase()
                : messageSource.getMessage(titleKey, titleArgs, status.getReasonPhrase(), locale);
        String detail = messageSource.getMessage(detailKey, detailArgs, locale);
        return build(status, title, detail, code, typeSuffixOrUri);
    }

    /** バリデーション等の配列プロパティを後付けする時用 */
    public ProblemDetail withProperty(ProblemDetail pd, String name, Object value) {
        pd.setProperty(name, value);
        return pd;
    }

    /** Securityハンドラ等で直接レスポンスへ書き込む */
    public void write(HttpServletResponse res, ProblemDetail pd, HttpStatus status) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(res.getWriter(), pd);
    }

    private URI resolveType(@Nullable String code, @Nullable String typeSuffixOrUri) {
        if (typeSuffixOrUri == null || typeSuffixOrUri.isBlank()) {
            // code があれば about:blank#<code小文字>、無ければ about:blank
            return (code == null) ? URI.create("about:blank")
                    : URI.create("about:blank#" + code.toLowerCase());
        }
        // suffix に # が付いていなければ about:blank#<suffix> とみなす
        if (typeSuffixOrUri.startsWith("http") || typeSuffixOrUri.startsWith("about:")) {
            return URI.create(typeSuffixOrUri);
        }
        return URI.create("about:blank#" + typeSuffixOrUri);
    }
}
