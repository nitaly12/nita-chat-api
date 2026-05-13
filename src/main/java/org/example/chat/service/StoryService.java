package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.config.FileStorageConfig;
import org.example.chat.dto.reponse.StoryDTO;
import org.example.chat.dto.reponse.UploadResponse;
import org.example.chat.entity.Friendship;
import org.example.chat.entity.Story;
import org.example.chat.entity.User;
import org.example.chat.repository.FriendshipRepository;
import org.example.chat.repository.StoryRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StoryService {

    private static final String STORIES_SUBDIR = "stories";
    private static final String UPLOADS_URL_PREFIX = "/uploads/";
    private static final Logger log = LoggerFactory.getLogger(StoryService.class);

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FileStorageService fileStorageService;
    private final FileStorageConfig fileStorageConfig;

    @Transactional
    public StoryDTO createStory(User author, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Story media must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Story media must be an image");
        }

        UploadResponse stored = fileStorageService.storeUnder(file, STORIES_SUBDIR);

        Story story = Story.builder()
                .userId(author.getId())
                .mediaUrl(stored.getUrl())
                .build();
        Story saved = storyRepository.save(story);
        return toDTO(saved, author);
    }

    @Transactional(readOnly = true)
    public List<StoryDTO> getFeed(User caller) {
        List<Story> stories = storyRepository.findLatestActiveStoryPerUserForFeed(
                caller.getId(), OffsetDateTime.now(ZoneOffset.UTC));
        return enrich(stories);
    }

    @Transactional(readOnly = true)
    public List<StoryDTO> getStoriesByUser(User caller, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + ownerId));
        if (!ownerId.equals(caller.getId()) && !areAcceptedFriends(caller.getId(), ownerId)) {
            throw new AccessDeniedException("You can only view stories of yourself or your friends");
        }
        List<Story> stories = storyRepository.findActiveByUserId(ownerId, OffsetDateTime.now(ZoneOffset.UTC));
        return stories.stream().map(s -> toDTO(s, owner)).toList();
    }

    @Transactional
    public void deleteStory(User caller, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new EntityNotFoundException("Story not found: " + storyId));
        if (!story.getUserId().equals(caller.getId())) {
            throw new AccessDeniedException("You can only delete your own stories");
        }
        String mediaUrl = story.getMediaUrl();
        storyRepository.delete(story);
        deleteMediaFileQuietly(mediaUrl);
    }

    private void deleteMediaFileQuietly(String mediaUrl) {
        Path file = resolveMediaPath(mediaUrl);
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete story file {}: {}", file, e.getMessage());
        }
    }

    private Path resolveMediaPath(String mediaUrl) {
        if (mediaUrl == null || !mediaUrl.startsWith(UPLOADS_URL_PREFIX)) return null;
        String relative = mediaUrl.substring(UPLOADS_URL_PREFIX.length());
        if (relative.isBlank()) return null;
        Path root = fileStorageConfig.getResolvedRoot();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) return null;
        return target;
    }

    private boolean areAcceptedFriends(Long a, Long b) {
        return friendshipRepository.findBetween(a, b)
                .map(f -> f.getStatus() == Friendship.Status.ACCEPTED)
                .orElse(false);
    }

    private List<StoryDTO> enrich(List<Story> stories) {
        if (stories.isEmpty()) return List.of();

        Set<Long> ownerIds = new HashSet<>();
        for (Story s : stories) ownerIds.add(s.getUserId());

        Map<Long, User> usersById = new HashMap<>();
        userRepository.findAllById(ownerIds).forEach(u -> usersById.put(u.getId(), u));

        return stories.stream()
                .map(s -> toDTO(s, usersById.get(s.getUserId())))
                .toList();
    }

    private StoryDTO toDTO(Story s, User owner) {
        if (owner == null) {
            owner = userRepository.findById(s.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + s.getUserId()));
        }
        return StoryDTO.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .displayName(owner.getDisplayName())
                .userAvatarUrl(owner.getAvatarUrl())
                .mediaUrl(s.getMediaUrl())
                .createdAt(s.getCreatedAt())
                .expiresAt(s.getExpiresAt())
                .build();
    }
}