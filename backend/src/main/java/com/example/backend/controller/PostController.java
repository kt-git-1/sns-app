package com.example.backend.controller;

import com.example.backend.dto.PostRequest;
import com.example.backend.dto.PostResponse;
import com.example.backend.security.AuthUser;
import com.example.backend.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    // ===== 投稿作成 =====
    @PostMapping
    public ResponseEntity<PostResponse> create(
            @Valid @RequestBody PostRequest req,
            @AuthenticationPrincipal AuthUser user
    ) {
        String username = user.username();
        PostResponse createdPost = posts.createPost(req, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    // ===== 投稿削除 =====
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthUser user
    ) {
        posts.deletePost(postId, user);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
