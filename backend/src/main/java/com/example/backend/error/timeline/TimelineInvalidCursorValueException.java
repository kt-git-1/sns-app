package com.example.backend.error.timeline;

import com.example.backend.error.DomainError;
import com.example.backend.error.DomainException;

public class TimelineInvalidCursorValueException extends DomainException {
    public  TimelineInvalidCursorValueException() {
        super(DomainError.TIMELINE_INVALID_CURSOR_VALUE, "timeline.invalid_cursor_value");
    }
}