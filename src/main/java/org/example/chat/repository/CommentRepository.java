package org.example.chat.repository;

import org.example.chat.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.post.id IN :postIds ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdsWithAuthor(@Param("postIds") Collection<Long> postIds);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("SELECT c.post.id AS postId, COUNT(c) AS cnt FROM Comment c " +
            "WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<Object[]> countByPostIdGrouped(@Param("postIds") Collection<Long> postIds);
}
