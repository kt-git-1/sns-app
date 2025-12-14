package com.example.backend.pagination;

import com.example.backend.error.timeline.TimelineInvalidCursorFormatException;
import com.example.backend.error.timeline.TimelineInvalidCursorValueException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Component
public class CursorCodec {

    public String encode(Cursor cursor) {
        if(cursor.createdAt() == null || cursor.id() == null) {
            return null;
        }

        long epochMillis = cursor.createdAt().toInstant().toEpochMilli();
        String raw = epochMillis + ":" + cursor.id();

        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Cursor decode(String encodedCursor) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedCursor);

            String raw = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if(parts.length != 2) {
                throw new TimelineInvalidCursorFormatException();
            }
            long epochMillis = Long.parseLong(parts[0]);
            long id = Long.parseLong(parts[1]);
            OffsetDateTime createdAt = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(epochMillis),
                    ZoneOffset.UTC
            );

            return new Cursor(createdAt, id);
        } catch (Exception e) {
            throw new TimelineInvalidCursorValueException();
        }
    }
}
