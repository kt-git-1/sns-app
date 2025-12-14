package com.example.backend.error.timeline;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class TimelineInvalidCursorFormatException extends DomainException {
    public  TimelineInvalidCursorFormatException() {
        super(DomainError.TIMELINE_INVALID_CURSOR_FORMAT, "timeline.invalid_cursor_format");
    }
}
