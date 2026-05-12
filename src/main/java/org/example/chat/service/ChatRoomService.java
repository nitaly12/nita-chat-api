package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.ChatRoomDTO;
import org.example.chat.dto.MemberDTO;
import org.example.chat.dto.MemberEvent;
import org.example.chat.dto.MessageDTO;
import org.example.chat.dto.ReactionSummary;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.MessageRepository;
import org.example.chat.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ReactionService reactionService;

    @Transactional(readOnly = true)
    public List<ChatRoomDTO> findRoomsForUser(User user) {
        return chatRoomRepository.findAllByMember(user).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ChatRoomDTO getOrCreatePrivateChat(User me, Long otherUserId) {
        if (me.getId().equals(otherUserId)) {
            throw new IllegalArgumentException("Cannot start a private chat with yourself");
        }
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + otherUserId));

        return chatRoomRepository.findPrivateChat(me, other)
                .map(this::toDto)
                .orElseGet(() -> {
                    ChatRoom room = new ChatRoom();
                    room.setGroup(false);
                    room.setName(me.getUsername() + "-" + other.getUsername());
                    Set<User> members = new HashSet<>();
                    members.add(me);
                    members.add(other);
                    room.setMembers(members);
                    return toDto(chatRoomRepository.save(room));
                });
    }

    @Transactional
    public ChatRoomDTO createGroup(String name, Set<Long> memberIds, User creator) {
        Set<User> members = new HashSet<>(userRepository.findAllById(memberIds));
        members.add(creator);
        if (members.size() < 2) {
            throw new IllegalArgumentException("A group chat needs at least 2 members");
        }

        ChatRoom room = new ChatRoom();
        room.setGroup(true);
        room.setName(name);
        room.setMembers(members);

        return toDto(chatRoomRepository.save(room));
    }

    @Transactional
    public ChatRoomDTO inviteToGroup(Long roomId, Long userId, User inviter) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
        if (!room.isGroup()) {
            throw new IllegalArgumentException("Cannot invite users to a 1:1 chat");
        }
        if (!isMember(room, inviter)) {
            throw new AccessDeniedException("Only members of the group can invite others");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (isMember(room, user)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }
        room.getMembers().add(user);
        ChatRoom saved = chatRoomRepository.save(room);
        broadcastMember(saved.getId(), MemberEvent.Type.JOINED, user);
        return toDto(saved);
    }

    @Transactional
    public void leaveGroup(Long roomId, User caller) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
        if (!room.isGroup()) {
            throw new IllegalArgumentException("Cannot leave a 1:1 chat");
        }
        if (!isMember(room, caller)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }
        User managed = userRepository.findById(caller.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + caller.getId()));
        room.getMembers().removeIf(m -> m.getId().equals(managed.getId()));
        chatRoomRepository.save(room);
        broadcastMember(roomId, MemberEvent.Type.LEFT, managed);
    }

    @Transactional
    public void removeMember(Long roomId, Long userId, User caller) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
        if (!room.isGroup()) {
            throw new IllegalArgumentException("Cannot remove members from a 1:1 chat");
        }
        if (!isMember(room, caller)) {
            throw new AccessDeniedException("Only members of the group can remove others");
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (!isMember(room, target)) {
            throw new IllegalArgumentException("User is not a member of this group");
        }
        room.getMembers().removeIf(m -> m.getId().equals(target.getId()));
        chatRoomRepository.save(room);
        broadcastMember(roomId, MemberEvent.Type.REMOVED, target);
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getHistory(Long roomId, User caller) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found: " + roomId));
        if (!isMember(room, caller)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }
        Map<Long, List<ReactionSummary>> reactionsByMessage =
                reactionService.getReactionsForRoom(roomId);
        return messageRepository.findHistoryByChatRoomId(roomId).stream()
                .map(msg -> MessageDTO.builder()
                        .id(msg.getId())
                        .content(msg.getContent())
                        .senderId(msg.getSender().getId())
                        .senderName(msg.getSender().getUsername())
                        .roomId(room.getId())
                        .createdAt(msg.getCreatedAt())
                        .edited(msg.isEdited())
                        .attachmentUrl(msg.getAttachmentUrl())
                        .attachmentType(msg.getAttachmentType())
                        .attachmentName(msg.getAttachmentName())
                        .reactions(reactionsByMessage.getOrDefault(msg.getId(), Collections.emptyList()))
                        .parentMessage(toQuote(msg.getParentMessage()))
                        .build())
                .toList();
    }

    private static final int PREVIEW_MAX = 120;

    private static MessageDTO.Quote toQuote(org.example.chat.entity.Message parent) {
        if (parent == null) return null;
        String content = parent.getContent() == null ? "" : parent.getContent();
        boolean hasContent = !content.isBlank();
        String snippet;
        if (!hasContent && parent.getAttachmentUrl() != null && !parent.getAttachmentUrl().isBlank()) {
            String type = parent.getAttachmentType();
            if (type != null && type.startsWith("image/")) snippet = "[Image]";
            else if (type != null && type.startsWith("video/")) snippet = "[Video]";
            else if (type != null && type.startsWith("audio/")) snippet = "[Audio]";
            else snippet = "[Attachment]";
        } else {
            snippet = content.length() > PREVIEW_MAX ? content.substring(0, PREVIEW_MAX) + "…" : content;
        }
        return MessageDTO.Quote.builder()
                .id(parent.getId())
                .senderName(parent.getSender().getUsername())
                .contentSnippet(snippet)
                .build();
    }

    private boolean isMember(ChatRoom room, User user) {
        return room.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
    }

    private void broadcastMember(Long roomId, MemberEvent.Type type, User user) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/members",
                MemberEvent.builder()
                        .type(type)
                        .roomId(roomId)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .build());
    }

    private ChatRoomDTO toDto(ChatRoom room) {
        List<MemberDTO> members = room.getMembers().stream()
                .map(u -> MemberDTO.builder().id(u.getId()).username(u.getUsername()).build())
                .toList();
        return ChatRoomDTO.builder()
                .id(room.getId())
                .name(room.getName())
                .group(room.isGroup())
                .createdAt(room.getCreatedAt())
                .members(members)
                .build();
    }
}
