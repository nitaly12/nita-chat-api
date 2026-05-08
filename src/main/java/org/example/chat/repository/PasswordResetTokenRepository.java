package org.example.chat.repository;

import org.example.chat.entity.PasswordResetToken;
import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PasswordResetToken prt where prt.user = :user")
    int deleteByUser(@Param("user") User user);
}
