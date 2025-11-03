package com.example.backend.error;

import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException {
    private final DomainError error;     // 例: USER_NOT_FOUND
    private final String messageKey;     // 例: user.not_found
    private final Object[] messageArgs;  // 例: {123L}

    protected DomainException(DomainError error, String messageKey, Object... messageArgs) {
        super(messageKey); // 実メッセージはHandler側で解決するため、キーを暫定格納
        this.error = error;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }
}
