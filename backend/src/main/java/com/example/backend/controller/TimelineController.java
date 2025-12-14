package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.pagination.PostPage;
import com.example.backend.service.TimelineService;
import com.example.backend.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
public class TimelineController {
    
    private final TimelineService timeline;

    public TimelineController(TimelineService timeline) {
        this.timeline = timeline;
    }

    @GetMapping("/timeline")
    public ResponseEntity<PostPage> getTimeline(
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(timeline.getTimelinePage(cursor, size));
    }

    @GetMapping("/users/me")
    public ResponseEntity<PostPage> getMyPage(
        @AuthenticationPrincipal AuthUser user,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(timeline.getUserPage(user.id(), cursor, size));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<PostPage> getUserPage(
        @PathVariable("userId") Long userId,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(timeline.getUserPage(userId, cursor, size));
    }
}
