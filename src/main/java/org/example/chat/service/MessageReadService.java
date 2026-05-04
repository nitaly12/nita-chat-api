package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.MemberReadDTO;
import org.example.chat.dto.ReadReceiptEvent;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.MessageRead;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.MessageReadRepository;
import org.example.chat.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageReadService {

    private final MessageReadRepository messageReadRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ReadReceiptEvent markRead(Long roomId, Long messageId, User caller) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found: " + roomId));
        if (!isMember(room, caller)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }
        User managedUser = userRepository.findById(caller.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + caller.getId()));

        MessageRead read = messageReadRepository
                .findByUserIdAndRoomId(caller.getId(), roomId)
                .orElseGet(() -> MessageRead.builder()
                        .user(managedUser)
                        .room(room)
                        .lastReadMessageId(messageId)
                        .build());

        if (read.getLastReadMessageId() == null || messageId > read.getLastReadMessageId()) {
            read.setLastReadMessageId(messageId);
        }
        read.setUpdatedAt(LocalDateTime.now());
        messageReadRepository.save(read);

        ReadReceiptEvent event = ReadReceiptEvent.builder()
                .roomId(roomId)
                .userId(managedUser.getId())
                .username(managedUser.getUsername())
                .lastReadMessageId(read.getLastReadMessageId())
                .readAt(read.getUpdatedAt())
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/reads", event);
        return event;
    }

    @Transactional(readOnly = true)
    public List<MemberReadDTO> getReads(Long roomId, User caller) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found: " + roomId));
        if (!isMember(room, caller)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }
        return messageReadRepository.findByRoomId(roomId).stream()
                .map(r -> MemberReadDTO.builder()
                        .userId(r.getUser().getId())
                        .username(r.getUser().getUsername())
                        .lastReadMessageId(r.getLastReadMessageId())
                        .lastReadAt(r.getUpdatedAt())
                        .build())
                .toList();
    }

    private boolean isMember(ChatRoom room, User user) {
        return room.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
    }
}
