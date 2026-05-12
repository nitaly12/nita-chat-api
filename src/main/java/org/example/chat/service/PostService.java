package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.CommentDTO;
import org.example.chat.dto.reponse.PostResponse;
import org.example.chat.dto.request.CreatePostRequest;
import org.example.chat.dto.request.UpdatePostRequest;
import org.example.chat.entity.Comment;
import org.example.chat.entity.Post;
import org.example.chat.entity.PostReaction;
import org.example.chat.entity.User;
import org.example.chat.repository.CommentRepository;
import org.example.chat.repository.PostReactionRepository;
import org.example.chat.repository.PostRepository;
import org.example.chat.repository.PostShareRepository;
import org.example.chat.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostReactionRepository reactionRepository;
    private final PostShareRepository shareRepository;

    @Transactional
    public PostResponse createPost(CreatePostRequest request, User author) {
        if (!userRepository.existsById(author.getId())) {
            throw new EntityNotFoundException("User not found: " + author.getId());
        }
        Post post = Post.builder()
                .userId(author.getId())
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .build();
        Post saved = postRepository.save(post);
        return toResponse(saved, true, 0L, 0L, 0L, null, Collections.emptyMap(), Collections.emptyList());
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request, User caller) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + postId));
        if (!post.getUserId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not the owner of this post");
        }
        post.setContent(request.getContent());
        post.setMediaUrl(request.getMediaUrl());
        return toResponseWithCounts(postRepository.save(post), true, caller);
    }

    @Transactional
    public void deletePost(Long postId, User caller) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + postId));
        if (!post.getUserId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not the owner of this post");
        }
        commentRepository.deleteByPostId(postId);
        reactionRepository.deleteByPostId(postId);
        shareRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUserId(Long userId, User caller) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        return enrich(postRepository.findByUserIdOrderByCreatedAtDesc(userId), caller);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(User caller) {
        return enrich(postRepository.findByUserIdOrderByCreatedAtDesc(caller.getId()), caller);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getFeed(User caller) {
        return enrich(postRepository.findFeedForUser(caller.getId()), caller);
    }

    private List<PostResponse> enrich(List<Post> posts, User caller) {
        if (posts.isEmpty()) return List.of();
        List<Long> ids = posts.stream().map(Post::getId).toList();

        Map<Long, Long> commentCounts = countMap(commentRepository.countByPostIdGrouped(ids));
        Map<Long, Long> shareCounts = countMap(shareRepository.countByPostIdGrouped(ids));
        Map<Long, Map<String, Long>> reactionCounts = reactionMap(reactionRepository.countByPostIdGroupedByEmoji(ids));
        Map<Long, List<CommentDTO>> commentsByPost = commentMap(
                commentRepository.findByPostIdsWithAuthor(ids), caller);
        Map<Long, String> myReactions = caller == null
                ? Collections.emptyMap()
                : myReactionMap(reactionRepository.findByUserIdAndPostIds(caller.getId(), ids));

        return posts.stream()
                .map(p -> {
                    Map<String, Long> summary = reactionCounts.getOrDefault(p.getId(), Collections.emptyMap());
                    long total = summary.values().stream().mapToLong(Long::longValue).sum();
                    return toResponse(
                            p,
                            caller != null && p.getUserId().equals(caller.getId()) ? Boolean.TRUE : null,
                            commentCounts.getOrDefault(p.getId(), 0L),
                            shareCounts.getOrDefault(p.getId(), 0L),
                            total,
                            myReactions.get(p.getId()),
                            summary,
                            commentsByPost.getOrDefault(p.getId(), Collections.emptyList()));
                })
                .toList();
    }

    private PostResponse toResponseWithCounts(Post p, Boolean isOwner, User caller) {
        long comments = commentRepository.countByPostId(p.getId());
        long shares = shareRepository.countByPostId(p.getId());
        Map<String, Long> reactions = PostReactionService.summarize(reactionRepository.findByPostId(p.getId()));
        long totalReactions = reactions.values().stream().mapToLong(Long::longValue).sum();
        String myReaction = null;
        if (caller != null) {
            List<PostReaction> mine = reactionRepository.findByPostIdAndUserIdOrderByIdDesc(p.getId(), caller.getId());
            if (!mine.isEmpty()) myReaction = mine.get(0).getEmoji();
        }
        List<CommentDTO> commentList = commentRepository.findByPostIdWithAuthor(p.getId()).stream()
                .map(c -> PostCommentService.toDto(c, caller))
                .toList();
        return toResponse(p, isOwner, comments, shares, totalReactions, myReaction, reactions, commentList);
    }

    private PostResponse toResponse(Post p, Boolean isOwner, long commentCount, long shareCount,
                                    long reactionCount, String myReaction,
                                    Map<String, Long> reactionSummary, List<CommentDTO> comments) {
        return PostResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .content(p.getContent())
                .mediaUrl(p.getMediaUrl())
                .createdAt(p.getCreatedAt())
                .isOwner(isOwner)
                .commentCount(commentCount)
                .shareCount(shareCount)
                .reactionCount(reactionCount)
                .myReaction(myReaction)
                .reactionSummary(reactionSummary)
                .comments(comments)
                .build();
    }

    private static Map<Long, String> myReactionMap(List<PostReaction> reactions) {
        Map<Long, String> out = new HashMap<>();
        for (PostReaction r : reactions) {
            out.putIfAbsent(r.getPostId(), r.getEmoji());
        }
        return out;
    }

    private static Map<Long, List<CommentDTO>> commentMap(List<Comment> rows, User caller) {
        Map<Long, List<CommentDTO>> out = new HashMap<>();
        for (Comment c : rows) {
            out.computeIfAbsent(c.getPost().getId(), k -> new ArrayList<>())
                    .add(PostCommentService.toDto(c, caller));
        }
        return out;
    }

    private static Map<Long, Long> countMap(List<Object[]> rows) {
        Map<Long, Long> out = new HashMap<>();
        for (Object[] row : rows) {
            out.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return out;
    }

    private static Map<Long, Map<String, Long>> reactionMap(List<Object[]> rows) {
        Map<Long, Map<String, Long>> out = new HashMap<>();
        for (Object[] row : rows) {
            Long postId = ((Number) row[0]).longValue();
            String emoji = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            out.computeIfAbsent(postId, k -> new LinkedHashMap<>()).put(emoji, count);
        }
        return out;
    }
}
