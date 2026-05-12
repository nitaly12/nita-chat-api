package org.example.chat.repository;

import org.example.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long roomId);

    Page<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.sender " +
            "LEFT JOIN FETCH m.parentMessage p " +
            "LEFT JOIN FETCH p.sender " +
            "WHERE m.chatRoom.id = :roomId " +
            "ORDER BY m.createdAt ASC")
    List<Message> findHistoryByChatRoomId(@Param("roomId") Long roomId);

    @Query("SELECT m FROM Message m JOIN m.chatRoom r JOIN r.members u " +
            "WHERE u.id = :userId " +
            "AND (:roomId IS NULL OR r.id = :roomId) " +
            "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY m.createdAt DESC")
    Page<Message> searchInUserRooms(@Param("userId") Long userId,
                                    @Param("roomId") Long roomId,
                                    @Param("q") String q,
                                    Pageable pageable);
}
