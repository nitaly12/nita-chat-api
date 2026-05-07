package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.PostReactionResponse;
import org.example.chat.entity.PostReaction;
import org.example.chat.entity.User;
import org.example.chat.repository.PostReactionRepository;
import org.example.chat.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostReactionService {

    private final PostRepository postRepository;
    private final PostReactionRepository reactionRepository;

    @Transactional
    public PostReactionResponse toggle(Long postId, String emoji, User caller) {
        if (!postRepository.existsById(postId)) {
            throw new EntityNotFoundException("Post not found: " + postId);
        }

        PostReactionResponse.Action action;
        Optional<PostReaction> existing =
                reactionRepository.findByPostIdAndUserIdAndEmoji(postId, caller.getId(), emoji);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            action = PostReactionResponse.Action.REMOVED;
        } else {
            reactionRepository.save(PostReaction.builder()
                    .postId(postId)
                    .userId(caller.getId())
                    .emoji(emoji)
                    .build());
            action = PostReactionResponse.Action.ADDED;
        }

        return PostReactionResponse.builder()
                .postId(postId)
                .emoji(emoji)
                .action(action)
                .reactionSummary(summarize(reactionRepository.findByPostId(postId)))
                .build();
    }

    static Map<String, Long> summarize(List<PostReaction> reactions) {
        Map<String, Long> grouped = new LinkedHashMap<>();
        for (PostReaction r : reactions) {
            grouped.merge(r.getEmoji(), 1L, Long::sum);
        }
        return grouped;
    }
}
