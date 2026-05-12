package org.example.chat.repository;

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

    @Query(value =
            "SELECT u.* FROM users u " +
            "WHERE u.id <> :currentId " +
            "AND u.id NOT IN (" +
            "    SELECT receiver_id FROM friendships " +
            "    WHERE requester_id = :currentId AND status IN ('ACCEPTED','PENDING')" +
            "    UNION" +
            "    SELECT requester_id FROM friendships " +
            "    WHERE receiver_id = :currentId AND status IN ('ACCEPTED','PENDING')" +
            ")",
            nativeQuery = true)
    List<User> findSuggestedFriends(@Param("currentId") Long currentId);
}
