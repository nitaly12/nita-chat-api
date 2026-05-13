package org.example.chat.repository;

import org.example.chat.dto.reponse.FriendDTO;
import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

    @Query("SELECT new org.example.chat.dto.reponse.FriendDTO(" +
            "    u.id, u.username, u.displayName, u.avatarUrl, " +
            "    COALESCE(fOut.status, fIn.status)) " +
            "FROM User u " +
            "LEFT JOIN Friendship fOut " +
            "    ON fOut.requesterId = :currentUserId AND fOut.receiverId = u.id " +
            "LEFT JOIN Friendship fIn " +
            "    ON fIn.requesterId = u.id AND fIn.receiverId = :currentUserId " +
            "WHERE u.id <> :currentUserId " +
            "ORDER BY u.id")
    List<FriendDTO> findSuggestedConnections(@Param("currentUserId") Long currentUserId);
}
