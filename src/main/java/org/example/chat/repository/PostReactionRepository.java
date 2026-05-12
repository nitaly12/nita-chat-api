package org.example.chat.repository;

import org.example.chat.entity.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    List<PostReaction> findByPostIdAndUserIdOrderByIdDesc(Long postId, Long userId);

    List<PostReaction> findByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("SELECT r.postId AS postId, r.emoji AS emoji, COUNT(r) AS cnt FROM PostReaction r " +
            "WHERE r.postId IN :postIds GROUP BY r.postId, r.emoji")
    List<Object[]> countByPostIdGroupedByEmoji(@Param("postIds") Collection<Long> postIds);

    @Query("SELECT r FROM PostReaction r WHERE r.userId = :userId AND r.postId IN :postIds " +
            "ORDER BY r.id DESC")
    List<PostReaction> findByUserIdAndPostIds(@Param("userId") Long userId,
                                              @Param("postIds") Collection<Long> postIds);
}
