package org.example.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.FriendDTO;
import org.example.chat.dto.reponse.FriendshipResponse;
import org.example.chat.dto.reponse.PendingFriendRequestDTO;
import org.example.chat.dto.request.FriendRequestPayload;
import org.example.chat.entity.User;
import org.example.chat.service.FriendshipService;
import org.example.chat.service.UserService;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/friendships")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<FriendshipResponse> sendRequest(
            @Valid @RequestBody FriendRequestPayload payload,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendshipService.sendRequest(caller, payload.getReceiverId()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<FriendshipResponse> accept(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(friendshipService.acceptRequest(caller, id));
    }

    @GetMapping
    public ResponseEntity<List<FriendDTO>> list(
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(friendshipService.getFriendList(caller));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingFriendRequestDTO>> pending(
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(friendshipService.getIncomingPending(caller));
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<PendingFriendRequestDTO>> incoming(
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(friendshipService.getIncomingPending(caller));
    }
}