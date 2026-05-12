package org.example.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.CommentDTO;
import org.example.chat.dto.reponse.PostReactionResponse;
import org.example.chat.dto.reponse.PostResponse;
import org.example.chat.dto.request.CreateCommentRequest;
import org.example.chat.dto.request.CreatePostRequest;
import org.example.chat.dto.request.ReactionRequest;
import org.example.chat.dto.request.UpdatePostRequest;
import org.example.chat.entity.PostShare;
import org.example.chat.entity.User;
import org.example.chat.service.PostCommentService;
import org.example.chat.service.PostReactionService;
import org.example.chat.service.PostService;
import org.example.chat.service.PostShareService;
import org.example.chat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostCommentService commentService;
    private final PostReactionService reactionService;
    private final PostShareService shareService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User author = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(request, author));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(postService.updatePost(postId, request, caller));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        postService.deletePost(postId, caller);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(postId, request, caller));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> listComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(commentService.list(postId, caller));
    }

    @PostMapping("/{id}/react")
    public ResponseEntity<PostReactionResponse> react(
            @PathVariable("id") Long postId,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(reactionService.toggle(postId, request.getEmoji(), caller));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<PostShare> share(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shareService.share(postId, caller));
    }

    @GetMapping("/me")
    public ResponseEntity<List<PostResponse>> getMyPosts(
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(postService.getMyPosts(caller));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getFeed(
            @AuthenticationPrincipal UserDetails principal) {
        User caller = userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(postService.getFeed(caller));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getUserPosts(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = principal == null ? null : userService.loadByUsername(principal.getUsername());
        return ResponseEntity.ok(postService.getPostsByUserId(userId, caller));
    }
}
