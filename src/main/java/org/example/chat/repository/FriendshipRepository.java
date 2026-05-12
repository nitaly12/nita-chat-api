package org.example.chat.repository;

import org.example.chat.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requesterId = :a AND f.receiverId = :b) OR " +
            "(f.requesterId = :b AND f.receiverId = :a)")
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("SELECT f FROM Friendship f WHERE f.status = org.example.chat.entity.Friendship.Status.ACCEPTED " +
            "AND (f.requesterId = :userId OR f.receiverId = :userId)")
    List<Friendship> findAcceptedForUser(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.receiverId = :userId " +
            "AND f.status = org.example.chat.entity.Friendship.Status.PENDING")
    List<Friendship> findIncomingPending(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.requesterId = :userId OR f.receiverId = :userId")
    List<Friendship> findAllInvolving(@Param("userId") Long userId);
}