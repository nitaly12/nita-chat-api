package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.ReactionEvent;
import org.example.chat.dto.ReactionSummary;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageReaction;
import org.example.chat.entity.User;
import org.example.chat.repository.MessageReactionRepository;
import org.example.chat.repository.MessageRepository;
import org.example.chat.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ReactionEvent toggle(Long messageId, String emoji, User caller) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        ChatRoom room = message.getChatRoom();
        if (!isMember(room, caller)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        ReactionEvent.Action action;
        String resultingEmoji = emoji;
        List<MessageReaction> existingRows =
                reactionRepository.findByMessageIdAndUserIdOrderByIdDesc(messageId, caller.getId());

        if (!existingRows.isEmpty()) {
            MessageReaction current = existingRows.get(0);
            if (existingRows.size() > 1) {
                reactionRepository.deleteAll(existingRows.subList(1, existingRows.size()));
            }
            if (current.getEmoji().equals(emoji)) {
                reactionRepository.delete(current);
                action = ReactionEvent.Action.REMOVED;
                resultingEmoji = null;
            } else {
                current.setEmoji(emoji);
                reactionRepository.save(current);
                action = ReactionEvent.Action.REPLACED;
            }
        } else {
            User managedUser = userRepository.findById(caller.getId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + caller.getId()));
            reactionRepository.save(MessageReaction.builder()
                    .message(message)
                    .user(managedUser)
                    .emoji(emoji)
                    .build());
            action = ReactionEvent.Action.ADDED;
        }

        Map<String, Long> counts = summarizeCounts(reactionRepository.findByMessageId(messageId));
        ReactionEvent event = ReactionEvent.builder()
                .action(action)
                .messageId(messageId)
                .roomId(room.getId())
                .emoji(resultingEmoji)
                .userId(caller.getId())
                .username(caller.getUsername())
                .reactionSummary(counts)
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + room.getId() + "/reactions", event);
        return event;
    }

    @Transactional
    public Map<String, Long> toggleReaction(Long userId, Long messageId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (!isMember(message.getChatRoom(), user)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        ReactionEvent.Action action;
        String resultingEmoji = emoji;
        List<MessageReaction> existingRows =
                reactionRepository.findByMessageIdAndUserIdOrderByIdDesc(messageId, userId);

        if (!existingRows.isEmpty()) {
            MessageReaction current = existingRows.get(0);
            if (existingRows.size() > 1) {
                reactionRepository.deleteAll(existingRows.subList(1, existingRows.size()));
            }
            if (current.getEmoji().equals(emoji)) {
                reactionRepository.delete(current);
                action = ReactionEvent.Action.REMOVED;
                resultingEmoji = null;
            } else {
                current.setEmoji(emoji);
                reactionRepository.save(current);
                action = ReactionEvent.Action.REPLACED;
            }
        } else {
            reactionRepository.save(MessageReaction.builder()
                    .message(message)
                    .user(user)
                    .emoji(emoji)
                    .build());
            action = ReactionEvent.Action.ADDED;
        }

        Map<String, Long> counts = summarizeCounts(reactionRepository.findByMessageId(messageId));
        Long roomId = message.getChatRoom().getId();
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/reactions",
                ReactionEvent.builder()
                        .action(action)
                        .messageId(messageId)
                        .roomId(roomId)
                        .emoji(resultingEmoji)
                        .userId(userId)
                        .username(user.getUsername())
                        .reactionSummary(counts)
                        .build());

        return counts;
    }

    @Transactional(readOnly = true)
    public List<ReactionSummary> getReactions(Long messageId, User caller) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        if (!isMember(message.getChatRoom(), caller)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }
        return summarize(reactionRepository.findByMessageId(messageId));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ReactionSummary>> getReactionsForRoom(Long roomId) {
        return reactionRepository.findByMessage_ChatRoomId(roomId).stream()
                .collect(Collectors.groupingBy(r -> r.getMessage().getId()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> summarize(e.getValue())));
    }

    private Map<String, Long> summarizeCounts(List<MessageReaction> reactions) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (MessageReaction r : reactions) {
            counts.merge(r.getEmoji(), 1L, Long::sum);
        }
        return counts;
    }

    private List<ReactionSummary> summarize(List<MessageReaction> reactions) {
        Map<String, ReactionSummary> grouped = new LinkedHashMap<>();
        for (MessageReaction r : reactions) {
            grouped.computeIfAbsent(r.getEmoji(), emoji -> ReactionSummary.builder()
                    .emoji(emoji)
                    .count(0)
                    .userIds(new java.util.ArrayList<>())
                    .build());
            ReactionSummary s = grouped.get(r.getEmoji());
            s.setCount(s.getCount() + 1);
            s.getUserIds().add(r.getUser().getId());
        }
        return new java.util.ArrayList<>(grouped.values());
    }

    private boolean isMember(ChatRoom room, User user) {
        return room.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
    }
}
