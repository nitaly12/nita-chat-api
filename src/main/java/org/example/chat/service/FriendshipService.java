package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.FriendDTO;
import org.example.chat.dto.reponse.FriendshipResponse;
import org.example.chat.dto.reponse.PendingFriendRequestDTO;
import org.example.chat.entity.Friendship;
import org.example.chat.entity.User;
import org.example.chat.repository.FriendshipRepository;
import org.example.chat.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional
    public FriendshipResponse sendRequest(User caller, Long receiverId) {
        if (receiverId == null || receiverId.equals(caller.getId())) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself");
        }
        if (!userRepository.existsById(receiverId)) {
            throw new EntityNotFoundException("User not found: " + receiverId);
        }
        friendshipRepository.findBetween(caller.getId(), receiverId).ifPresent(existing -> {
            String msg = existing.getStatus() == Friendship.Status.ACCEPTED
                    ? "You are already friends"
                    : "A friend request between these users already exists";
            throw new IllegalStateException(msg);
        });

        Friendship saved = friendshipRepository.save(Friendship.builder()
                .requesterId(caller.getId())
                .receiverId(receiverId)
                .status(Friendship.Status.PENDING)
                .build());
        return toResponse(saved);
    }

    @Transactional
    public FriendshipResponse acceptRequest(User caller, Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new EntityNotFoundException("Friend request not found: " + friendshipId));
        if (!f.getReceiverId().equals(caller.getId())) {
            throw new AccessDeniedException("Only the receiver can accept this request");
        }
        if (f.getStatus() != Friendship.Status.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }
        f.setStatus(Friendship.Status.ACCEPTED);
        f.setRespondedAt(LocalDateTime.now());
        return toResponse(friendshipRepository.save(f));
    }

    @Transactional(readOnly = true)
    public List<FriendDTO> getFriendList(User caller) {
        List<Friendship> accepted = friendshipRepository.findAcceptedForUser(caller.getId());
        Set<Long> friendIds = new HashSet<>();
        for (Friendship f : accepted) {
            friendIds.add(f.getRequesterId().equals(caller.getId()) ? f.getReceiverId() : f.getRequesterId());
        }
        if (friendIds.isEmpty()) return List.of();
        return userRepository.findAllById(friendIds).stream()
                .map(u -> FriendDTO.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getDisplayName())
                        .avatarUrl(u.getAvatarUrl())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingFriendRequestDTO> getIncomingPending(User caller) {
        List<Friendship> pending = friendshipRepository.findIncomingPending(caller.getId());
        if (pending.isEmpty()) return List.of();

        Set<Long> requesterIds = new HashSet<>();
        for (Friendship f : pending) requesterIds.add(f.getRequesterId());

        Map<Long, org.example.chat.entity.User> usersById = new HashMap<>();
        userRepository.findAllById(requesterIds).forEach(u -> usersById.put(u.getId(), u));

        return pending.stream()
                .map(f -> {
                    org.example.chat.entity.User u = usersById.get(f.getRequesterId());
                    return PendingFriendRequestDTO.builder()
                            .id(f.getId())
                            .requesterId(f.getRequesterId())
                            .requesterUsername(u != null ? u.getUsername() : null)
                            .requesterDisplayName(u != null ? u.getDisplayName() : null)
                            .requesterAvatarUrl(u != null ? u.getAvatarUrl() : null)
                            .createdAt(f.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> getAcceptedFriendIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        for (Friendship f : friendshipRepository.findAcceptedForUser(userId)) {
            ids.add(f.getRequesterId().equals(userId) ? f.getReceiverId() : f.getRequesterId());
        }
        return ids;
    }

    private FriendshipResponse toResponse(Friendship f) {
        return FriendshipResponse.builder()
                .id(f.getId())
                .requesterId(f.getRequesterId())
                .receiverId(f.getReceiverId())
                .status(f.getStatus())
                .createdAt(f.getCreatedAt())
                .respondedAt(f.getRespondedAt())
                .build();
    }
}