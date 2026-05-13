package org.example.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.StoryDTO;
import org.example.chat.entity.User;
import org.example.chat.service.StoryService;
import org.example.chat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final UserService userService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoryDTO> createStory(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) throws IOException {
        User author = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storyService.createStory(author, file));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<StoryDTO>> getFeed(@AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(storyService.getFeed(caller));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<StoryDTO>> getStoriesByUser(
            @PathVariable("id") Long ownerId,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(storyService.getStoriesByUser(caller, ownerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(
            @PathVariable("id") Long storyId,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        storyService.deleteStory(caller, storyId);
        return ResponseEntity.noContent().build();
    }
}