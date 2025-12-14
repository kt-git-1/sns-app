package com.example.backend.repository;

import com.example.backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.user
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findFirstPage(Pageable pageable);


    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.user
        WHERE p.createdAt < :createdAt
        OR (p.createdAt = :createdAt AND p.id < :id)
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    List<Post> findNextPage(
            @Param("createdAt") OffsetDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.user u
        WHERE u.id = :userId
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findUserFirstPage(
        @Param("userId") Long userId,
        Pageable pageable
    );


    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.user u
        WHERE u.id = :userId
        AND (p.createdAt < :createdAt
        OR (p.createdAt = :createdAt AND p.id < :id))
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    List<Post> findUserNextPage(
            @Param("userId") Long userId,
            @Param("createdAt") OffsetDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable
    );
}
