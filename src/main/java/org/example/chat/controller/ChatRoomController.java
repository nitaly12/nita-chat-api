package org.example.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.MemberReadDTO;
import org.example.chat.dto.MessageDTO;
import org.example.chat.dto.ReadReceiptEvent;
import org.example.chat.dto.request.MarkReadRequest;
import org.example.chat.entity.User;
import org.example.chat.service.ChatRoomService;
import org.example.chat.service.MessageReadService;
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
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final MessageReadService messageReadService;
    private final UserService userService;

    @GetMapping("/{roomId}/history")
    public ResponseEntity<List<MessageDTO>> getHistory(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(chatRoomService.getHistory(roomId, me));
    }

    @PostMapping("/{roomId}/read")
    public ResponseEntity<ReadReceiptEvent> markRead(
            @PathVariable Long roomId,
            @Valid @RequestBody MarkReadRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(messageReadService.markRead(roomId, request.getMessageId(), me));
    }

    @GetMapping("/{roomId}/reads")
    public ResponseEntity<List<MemberReadDTO>> getReads(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(messageReadService.getReads(roomId, me));
    }
}
