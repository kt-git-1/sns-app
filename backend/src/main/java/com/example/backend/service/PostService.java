package com.example.backend.service;

import com.example.backend.dto.PostRequest;
import com.example.backend.dto.PostResponse;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.error.posts.PostActionForbiddenException;
import com.example.backend.error.posts.PostNotFoundException;
import com.example.backend.error.posts.PostTooLongException;
import com.example.backend.error.users.UserNotFoundException;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private static final int CONTENT_MAX_LENGTH = 280;

    private final PostRepository posts;
    private final UserRepository users;

    public PostService(PostRepository posts, UserRepository users) {
        this.posts = posts;
        this.users = users;
    }

    // ===== 投稿作成 =====
    @Transactional
    public PostResponse createPost(PostRequest req, String username) {
        User user = users.findByUsername(username)
                         .orElseThrow(() -> new UserNotFoundException(username));

        if(req.content().length() > CONTENT_MAX_LENGTH) {
            throw new PostTooLongException(CONTENT_MAX_LENGTH, req.content().length());
        }

        Post post = new Post();
        post.setContent(req.content());
        post.setUser(user);
        posts.save(post); // DBに保存

        return new PostResponse(
                post.getId(),
                post.getContent(),
                user.getUsername(),
                post.getCreatedAt()
        );
    }

    // ===== 投稿削除 =====
    @Transactional
    public void deletePost(
            Long postId,
            @AuthenticationPrincipal AuthUser user
    ) {
        Post post = posts.findByPostId(postId)
                .orElseThrow(PostNotFoundException::new);

        if(!post.getUser().getId().equals(user.id())) {
            throw new PostActionForbiddenException();
        }

        posts.delete(post);
    }
}
