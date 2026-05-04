package org.example.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.ChatRoomDTO;
import org.example.chat.dto.request.CreateGroupRequest;
import org.example.chat.entity.User;
import org.example.chat.service.ChatRoomService;
import org.example.chat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ChatRoomDTO>> myChats(@AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(chatRoomService.findRoomsForUser(me));
    }

    @PostMapping("/private/{otherUserId}")
    public ResponseEntity<ChatRoomDTO> getOrCreatePrivateChat(
            @PathVariable Long otherUserId,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(chatRoomService.getOrCreatePrivateChat(me, otherUserId));
    }

    @PostMapping("/group")
    public ResponseEntity<ChatRoomDTO> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        ChatRoomDTO room = chatRoomService.createGroup(request.getName(), request.getMemberIds(), me);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @PostMapping("/{roomId}/invite/{userId}")
    public ResponseEntity<ChatRoomDTO> invite(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(chatRoomService.inviteToGroup(roomId, userId, me));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leave(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        chatRoomService.leaveGroup(roomId, me);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.loadByUsername(principal.getUsername());
        chatRoomService.removeMember(roomId, userId, me);
        return ResponseEntity.noContent().build();
    }
}
