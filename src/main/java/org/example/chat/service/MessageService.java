package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.MessageDeletedEvent;
import org.example.chat.dto.reponse.ChatNotificationDTO;
import org.example.chat.dto.reponse.MessageResponse;
import org.example.chat.dto.request.MessageRequest;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.MessageRepository;
import org.example.chat.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int PREVIEW_MAX = 120;

    @Transactional
    public MessageResponse sendMessage(MessageRequest request, User sender) {
        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + request.getRoomId()));

        User managedSender = userRepository.findById(sender.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + sender.getId()));

        boolean isMember = room.getMembers().stream()
                .anyMatch(m -> m.getId().equals(managedSender.getId()));
        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasAttachment = request.getAttachmentUrl() != null && !request.getAttachmentUrl().isBlank();
        if (!hasContent && !hasAttachment) {
            throw new IllegalArgumentException("Message must have content or an attachment");
        }

        Message message = Message.builder()
                .chatRoom(room)
                .sender(managedSender)
                .content(request.getContent() == null ? "" : request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentType(request.getAttachmentType())
                .attachmentName(request.getAttachmentName())
                .build();

        Message saved = messageRepository.save(message);
        MessageResponse response = toResponse(saved);
        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), response);

        room.getMembers().forEach(member -> notifyRecipient(member, managedSender, room, saved));

        return response;
    }

    private void notifyRecipient(User recipient, User sender, ChatRoom room, Message message) {
        if (recipient == null || recipient.getId().equals(sender.getId())) {
            return;
        }

        ChatNotificationDTO payload = ChatNotificationDTO.builder()
                .chatId(room.getId())
                .senderName(sender.getDisplayName() != null ? sender.getDisplayName() : sender.getUsername())
                .senderAvatar(sender.getAvatarUrl())
                .contentPreview(buildPreview(message))
                .sentAt(message.getCreatedAt())
                .build();

        messagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/notifications",
                payload);
    }

    private String buildPreview(Message m) {
        boolean hasContent = m.getContent() != null && !m.getContent().isBlank();
        if (!hasContent && m.getAttachmentUrl() != null && !m.getAttachmentUrl().isBlank()) {
            String type = m.getAttachmentType();
            if (type != null && type.startsWith("image/")) return "[Image]";
            if (type != null && type.startsWith("video/")) return "[Video]";
            if (type != null && type.startsWith("audio/")) return "[Audio]";
            return "[Attachment]";
        }
        String c = m.getContent() == null ? "" : m.getContent();
        return c.length() > PREVIEW_MAX ? c.substring(0, PREVIEW_MAX) + "…" : c;
    }

    @Transactional
    public MessageResponse editMessage(Long messageId, String newContent, User caller) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        if (!message.getSender().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Only the sender can edit this message");
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setUpdatedAt(LocalDateTime.now());
        MessageResponse response = toResponse(messageRepository.save(message));
        messagingTemplate.convertAndSend(
                "/topic/room/" + message.getChatRoom().getId() + "/edited", response);
        return response;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MessageResponse> search(
            String query, Long roomId, User caller,
            org.springframework.data.domain.Pageable pageable) {
        if (query == null || query.isBlank()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        return messageRepository
                .searchInUserRooms(caller.getId(), roomId, query.trim(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void deleteMessage(Long messageId, User caller) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        if (!message.getSender().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Only the sender can delete this message");
        }

        Long roomId = message.getChatRoom().getId();
        messageRepository.delete(message);
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/deleted",
                MessageDeletedEvent.builder().messageId(messageId).roomId(roomId).build());
    }

    private MessageResponse toResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .content(m.getContent())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getUsername())
                .roomId(m.getChatRoom().getId())
                .createdAt(m.getCreatedAt())
                .edited(m.isEdited())
                .attachmentUrl(m.getAttachmentUrl())
                .attachmentType(m.getAttachmentType())
                .attachmentName(m.getAttachmentName())
                .build();
    }
}
