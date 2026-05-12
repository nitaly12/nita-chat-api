package org.example.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.UploadResponse;
import org.example.chat.dto.reponse.UserResponse;
import org.example.chat.service.FileStorageService;
import org.example.chat.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    @PutMapping("/profile/update")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestParam("bio") String bio,
            @RequestParam("theme") String theme,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            Principal principal) throws IOException {

        String coverUrl = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            String ct = coverImage.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                throw new IllegalArgumentException("Cover image must be an image file");
            }
            UploadResponse stored = fileStorageService.storeUnder(coverImage, "covers");
            coverUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(stored.getUrl())
                    .toUriString();
        }

        return ResponseEntity.ok(
                userService.updateBioCoverAndTheme(principal.getName(), bio, theme, coverUrl));
    }
}
