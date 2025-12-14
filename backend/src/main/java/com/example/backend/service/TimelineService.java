package com.example.backend.service;

import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.pagination.Cursor;
import com.example.backend.pagination.CursorCodec;
import com.example.backend.pagination.PostPage;
import com.example.backend.pagination.PostSummaryDto;
import com.example.backend.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TimelineService {

    private final PostRepository posts;
    private final CursorCodec cursorCodec;

    public TimelineService(PostRepository posts, CursorCodec cursorCodec) {
        this.posts = posts;
        this.cursorCodec = cursorCodec;
    }

    @Transactional(readOnly = true)
    public PostPage getTimelinePage(String cursor, int size) {
        Cursor c = decodeCursor(cursor);
        List<Post> result;
        if(c.createdAt() == null || c.id() == null) {
            result = posts.findFirstPage(PageRequest.of(0, size + 1));
        } else {
            result = posts.findNextPage(c.createdAt(), c.id(), PageRequest.of(0, size + 1));
        }

        return toPostPage(result, size);
    }

    @Transactional(readOnly = true)
    public PostPage getUserPage(Long userId, String cursor, int size) {
        Cursor c = decodeCursor(cursor);
        List<Post> result;
        if(c.createdAt() == null || c.id() == null) {
            result = posts.findUserFirstPage(userId, PageRequest.of(0, size + 1));
        } else {
            result = posts.findUserNextPage(userId, c.createdAt(), c.id(), PageRequest.of(0, size + 1));
        }

        return toPostPage(result, size);
    }

    private Cursor decodeCursor(String cursor) {
        if(cursor == null || cursor.isBlank()) {
            return new Cursor(null, null);
        }
        return cursorCodec.decode(cursor);
    }

    private PostPage toPostPage(List<Post> result, int size) {
        boolean hasNext = result.size() > size;
        List<Post> pagePosts = hasNext ? result.subList(0, size) : result;

        List<PostSummaryDto> dtos = pagePosts.stream()
                                             .map(this::toDto)
                                             .toList();

        String nextCursor = null;
        if(hasNext && !pagePosts.isEmpty()) {
            Post last = pagePosts.get(pagePosts.size() - 1);
            Cursor cursor = new Cursor(last.getCreatedAt(), last.getId());
            nextCursor = cursorCodec.encode(cursor);
        }

        return new PostPage(dtos, nextCursor, hasNext);
    }

    private PostSummaryDto toDto(Post post) {
        User user = post.getUser();
        return new PostSummaryDto(
                post.getId(),
                user.getId(),
                user.getUsername(),
                post.getContent(),
                post.getCreatedAt()
        );
    }


}
