package org.example.chat.repository;

import org.example.chat.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    List<MessageReaction> findByMessageId(Long messageId);

    List<MessageReaction> findByMessage_ChatRoomId(Long chatRoomId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MessageReaction r where r.message.id = :messageId")
    int deleteAllByMessageId(@Param("messageId") Long messageId);
}
