package org.example.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.ReactionEvent;
import org.example.chat.dto.ReactionSummary;
import org.example.chat.dto.request.ReactionRequest;
import org.example.chat.entity.User;
import org.example.chat.service.ReactionService;
import org.example.chat.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages/{messageId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ReactionEvent> toggle(
            @PathVariable Long messageId,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(reactionService.toggle(messageId, request.getEmoji(), me));
    }

    @GetMapping
    public ResponseEntity<List<ReactionSummary>> list(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(reactionService.getReactions(messageId, me));
    }
}
