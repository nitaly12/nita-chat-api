package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.CommentDTO;
import org.example.chat.dto.request.CreateCommentRequest;
import org.example.chat.dto.request.UpdateCommentRequest;
import org.example.chat.entity.Comment;
import org.example.chat.entity.Post;
import org.example.chat.entity.User;
import org.example.chat.repository.CommentRepository;
import org.example.chat.repository.PostRepository;
import org.example.chat.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentDTO create(Long postId, CreateCommentRequest request, User caller) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + postId));
        User author = userRepository.findById(caller.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + caller.getId()));

        Comment.CommentBuilder builder = Comment.builder()
                .post(post)
                .author(author)
                .content(request.getContent());

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent comment not found: " + request.getParentId()));
            if (!parent.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("Parent comment does not belong to this post");
            }
            builder.parent(parent);
        }

        return toDto(commentRepository.save(builder.build()), caller);
    }

    @Transactional
    public CommentDTO update(Long commentId, UpdateCommentRequest request, User caller) {
        Comment comment = loadOwned(commentId, caller);
        comment.setContent(request.getContent());
        return toDto(commentRepository.save(comment), caller);
    }

    @Transactional
    public void delete(Long commentId, User caller) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));

        Long callerId = caller.getId();
        boolean isAuthor = comment.getAuthor().getId().equals(callerId);
        boolean isPostOwner = comment.getPost().getUserId().equals(callerId);
        if (!isAuthor && !isPostOwner) {
            throw new AccessDeniedException("You are not allowed to delete this comment");
        }
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> list(Long postId, User caller) {
        if (!postRepository.existsById(postId)) {
            throw new EntityNotFoundException("Post not found: " + postId);
        }
        List<Comment> all = commentRepository.findByPostIdWithAuthor(postId);

        Map<Long, CommentDTO> byId = new LinkedHashMap<>();
        for (Comment c : all) {
            byId.put(c.getId(), toDto(c, caller));
        }

        List<CommentDTO> roots = new ArrayList<>();
        for (Comment c : all) {
            CommentDTO dto = byId.get(c.getId());
            Comment parent = c.getParent();
            if (parent == null) {
                roots.add(dto);
            } else {
                CommentDTO parentDto = byId.get(parent.getId());
                if (parentDto != null) {
                    parentDto.getReplies().add(dto);
                } else {
                    roots.add(dto);
                }
            }
        }
        return roots;
    }

    private Comment loadOwned(Long commentId, User caller) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        if (!comment.getAuthor().getId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not the owner of this comment");
        }
        return comment;
    }

    static CommentDTO toDto(Comment c, User caller) {
        Long authorId = c.getAuthor().getId();
        Comment parent = c.getParent();
        return CommentDTO.builder()
                .id(c.getId())
                .postId(c.getPost().getId())
                .parentCommentId(parent != null ? parent.getId() : null)
                .authorId(authorId)
                .username(c.getAuthor().getUsername())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .isOwner(caller != null && authorId.equals(caller.getId()))
                .replies(new ArrayList<>())
                .build();
    }
}
