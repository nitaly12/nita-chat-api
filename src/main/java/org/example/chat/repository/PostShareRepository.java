package org.example.chat.repository;

import org.example.chat.entity.PostShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostShareRepository extends JpaRepository<PostShare, Long> {

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("SELECT s.postId AS postId, COUNT(s) AS cnt FROM PostShare s " +
            "WHERE s.postId IN :postIds GROUP BY s.postId")
    List<Object[]> countByPostIdGrouped(@Param("postIds") Collection<Long> postIds);
}
