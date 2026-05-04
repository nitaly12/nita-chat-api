package org.example.chat.repository;

import org.example.chat.entity.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    Optional<MessageRead> findByUserIdAndRoomId(Long userId, Long roomId);

    List<MessageRead> findByRoomId(Long roomId);
}
