package org.example.chat.repository;

import org.example.chat.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT p FROM Post p WHERE p.userId = :userId " +
            "OR p.userId IN (" +
            "  SELECT f.receiverId FROM Friendship f " +
            "  WHERE f.requesterId = :userId " +
            "  AND f.status = org.example.chat.entity.Friendship.Status.ACCEPTED" +
            ") " +
            "OR p.userId IN (" +
            "  SELECT f.requesterId FROM Friendship f " +
            "  WHERE f.receiverId = :userId " +
            "  AND f.status = org.example.chat.entity.Friendship.Status.ACCEPTED" +
            ") " +
            "ORDER BY p.createdAt DESC")
    List<Post> findFeedForUser(@Param("userId") Long userId);
}
