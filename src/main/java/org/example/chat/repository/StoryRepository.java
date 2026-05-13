package org.example.chat.repository;

import org.example.chat.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("SELECT s FROM Story s WHERE s.expiresAt > :now " +
            "AND s.mediaUrl IS NOT NULL " +
            "AND (" +
            "s.userId = :userId " +
            "OR s.userId IN (" +
            "  SELECT f.receiverId FROM Friendship f " +
            "  WHERE f.requesterId = :userId " +
            "  AND f.status = org.example.chat.entity.Friendship.Status.ACCEPTED" +
            ") " +
            "OR s.userId IN (" +
            "  SELECT f.requesterId FROM Friendship f " +
            "  WHERE f.receiverId = :userId " +
            "  AND f.status = org.example.chat.entity.Friendship.Status.ACCEPTED" +
            ")) " +
            "AND NOT EXISTS (" +
            "  SELECT s2 FROM Story s2 " +
            "  WHERE s2.userId = s.userId " +
            "  AND s2.expiresAt > :now " +
            "  AND s2.mediaUrl IS NOT NULL " +
            "  AND s2.createdAt > s.createdAt" +
            ") " +
            "ORDER BY s.createdAt DESC")
    List<Story> findLatestActiveStoryPerUserForFeed(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @Query("SELECT s FROM Story s " +
            "WHERE s.userId = :ownerId " +
            "AND s.expiresAt > :now " +
            "AND s.mediaUrl IS NOT NULL " +
            "ORDER BY s.createdAt DESC")
    List<Story> findActiveByUserId(@Param("ownerId") Long ownerId, @Param("now") OffsetDateTime now);
}