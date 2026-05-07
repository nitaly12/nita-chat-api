package org.example.chat.repository;

import org.example.chat.entity.RefreshToken;
import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshToken rt where rt.user = :user")
    int deleteByUser(@Param("user") User user);
}
