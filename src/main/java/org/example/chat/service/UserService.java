package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.FriendDTO;
import org.example.chat.dto.reponse.PublicProfileResponse;
import org.example.chat.dto.reponse.UploadResponse;
import org.example.chat.dto.reponse.UserResponse;
import org.example.chat.dto.request.ProfileSettingsForm;
import org.example.chat.dto.request.UpdateProfileRequest;
import org.example.chat.entity.Friendship;
import org.example.chat.entity.User;
import org.example.chat.repository.FriendshipRepository;
import org.example.chat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsersExcept(String currentUsername) {
        User current = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUsername));
        Map<Long, FriendshipView> viewByUserId = friendshipViewMap(current.getId());
        FriendshipView none = new FriendshipView("NONE", null);
        return userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(currentUsername))
                .map(u -> {
                    FriendshipView v = viewByUserId.getOrDefault(u.getId(), none);
                    return toResponse(u, v.status(), v.requesterId());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendDTO> getSuggestedConnections(String currentUsername) {
        User current = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUsername));
        return userRepository.findSuggestedConnections(current.getId());
    }

    private record FriendshipView(String status, Long requesterId) {}

    private Map<Long, FriendshipView> friendshipViewMap(Long currentId) {
        Map<Long, FriendshipView> map = new HashMap<>();
        for (Friendship f : friendshipRepository.findAllInvolving(currentId)) {
            Long other = f.getRequesterId().equals(currentId) ? f.getReceiverId() : f.getRequesterId();
            if (f.getStatus() == Friendship.Status.ACCEPTED) {
                map.put(other, new FriendshipView("ACCEPTED", f.getRequesterId()));
            } else if (f.getStatus() == Friendship.Status.PENDING) {
                String s = f.getRequesterId().equals(currentId) ? "PENDING_OUT" : "PENDING_IN";
                map.put(other, new FriendshipView(s, f.getRequesterId()));
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return PublicProfileResponse.builder()
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .build();
    }

    @Transactional(readOnly = true)
    public User loadByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest req) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        if (req.getDisplayName() != null) {
            u.setDisplayName(req.getDisplayName().isBlank() ? null : req.getDisplayName());
        }
        if (req.getBio() != null) {
            u.setBio(req.getBio().isBlank() ? null : req.getBio());
        }
        if (req.getAvatarUrl() != null) {
            u.setAvatarUrl(req.getAvatarUrl().isBlank() ? null : req.getAvatarUrl());
        }
        if (req.getTheme() != null && !req.getTheme().isBlank()) {
            u.setTheme(req.getTheme());
        }
        return toResponse(userRepository.save(u));
    }

    @Transactional
    public UserResponse setAvatar(String username, String avatarUrl) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        u.setAvatarUrl(avatarUrl);
        return toResponse(userRepository.save(u));
    }

    @Transactional
    public UserResponse removeAvatar(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        u.setAvatarUrl(null);
        return toResponse(userRepository.save(u));
    }

    @Transactional
    public UserResponse updateBioAndCover(String currentUsername, String bio, String coverPhotoUrl) {
        return updateBioCoverAndTheme(currentUsername, bio, null, coverPhotoUrl);
    }

    @Transactional
    public UserResponse updateBioCoverAndTheme(String currentUsername,
                                               String bio,
                                               String theme,
                                               String coverPhotoUrl) {
        User u = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUsername));
        if (bio != null) {
            u.setBio(bio.isBlank() ? null : bio);
        }
        if (theme != null && !theme.isBlank()) {
            if (!theme.equals("light") && !theme.equals("dark")) {
                throw new IllegalArgumentException("theme must be 'light' or 'dark'");
            }
            u.setTheme(theme);
        }
        if (coverPhotoUrl != null) {
            u.setCoverPhotoUrl(coverPhotoUrl);
        }
        return toResponse(userRepository.save(u));
    }

    @Transactional
    public UserResponse updateProfileSettings(String currentUsername,
                                              ProfileSettingsForm form,
                                              MultipartFile avatar,
                                              MultipartFile coverPhoto) throws IOException {
        User u = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUsername));

        if (form.getDisplayName() != null) {
            u.setDisplayName(form.getDisplayName().isBlank() ? null : form.getDisplayName());
        }
        if (form.getUsername() != null && !form.getUsername().isBlank()
                && !form.getUsername().equals(u.getUsername())) {
            if (userRepository.existsByUsername(form.getUsername())) {
                throw new IllegalStateException("Username already taken");
            }
            u.setUsername(form.getUsername());
        }
        if (form.getBio() != null) {
            u.setBio(form.getBio().isBlank() ? null : form.getBio());
        }
        if (form.getTheme() != null && !form.getTheme().isBlank()) {
            u.setTheme(form.getTheme());
        }
        if (avatar != null && !avatar.isEmpty()) {
            requireImage(avatar, "Profile image");
            UploadResponse stored = fileStorageService.store(avatar);
            u.setAvatarUrl(stored.getUrl());
        }
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            requireImage(coverPhoto, "Cover photo");
            UploadResponse stored = fileStorageService.store(coverPhoto);
            u.setCoverPhotoUrl(stored.getUrl());
        }
        return toResponse(userRepository.save(u));
    }

    private void requireImage(MultipartFile file, String label) {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException(label + " must be an image");
        }
    }

    private UserResponse toResponse(User user) {
        return toResponse(user, null, null);
    }

    private UserResponse toResponse(User user, String friendshipStatus, Long requesterId) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .online(user.isOnline())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .coverPhotoUrl(user.getCoverPhotoUrl())
                .bio(user.getBio())
                .theme(user.getTheme())
                .lastSeenAt(user.getLastSeenAt())
                .friendshipStatus(friendshipStatus)
                .requesterId(requesterId)
                .build();
    }
}
