package org.example.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.CommentDTO;
import org.example.chat.dto.request.UpdateCommentRequest;
import org.example.chat.entity.User;
import org.example.chat.service.PostCommentService;
import org.example.chat.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final PostCommentService commentService;
    private final UserService userService;

    @PutMapping("/{id}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(commentService.update(id, request, caller));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        commentService.delete(id, caller);
        return ResponseEntity.noContent().build();
    }
}
