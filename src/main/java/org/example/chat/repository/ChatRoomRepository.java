package org.example.chat.repository;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
           SELECT r FROM ChatRoom r
           WHERE r.group = false
             AND :u1 MEMBER OF r.members
             AND :u2 MEMBER OF r.members
             AND SIZE(r.members) = 2
           """)
    Optional<ChatRoom> findPrivateChat(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT r FROM ChatRoom r WHERE :user MEMBER OF r.members ORDER BY r.createdAt DESC")
    List<ChatRoom> findAllByMember(@Param("user") User user);

    @Query("""
           SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
           FROM ChatRoom r
           WHERE r.id = :roomId AND :user MEMBER OF r.members
           """)
    boolean isMember(@Param("roomId") Long roomId, @Param("user") User user);
}
