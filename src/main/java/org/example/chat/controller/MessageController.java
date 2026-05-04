package org.example.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.MessageResponse;
import org.example.chat.dto.request.MessageEditRequest;
import org.example.chat.dto.request.MessageRequest;
import org.example.chat.entity.User;
import org.example.chat.service.MessageService;
import org.example.chat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User sender = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(request, sender));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody MessageEditRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(messageService.editMessage(messageId, request.getContent(), caller));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        messageService.deleteMessage(messageId, caller);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MessageResponse>> search(
            @RequestParam("q") String q,
            @RequestParam(value = "roomId", required = false) Long roomId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(
                messageService.search(q, roomId, caller, PageRequest.of(Math.max(page, 0), safeSize)));
    }
}
