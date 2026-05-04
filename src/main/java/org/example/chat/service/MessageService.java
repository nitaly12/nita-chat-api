package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.MessageDeletedEvent;
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

        MessageResponse response = toResponse(messageRepository.save(message));
        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), response);
        return response;
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
