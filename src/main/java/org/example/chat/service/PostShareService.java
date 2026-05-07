package org.example.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.entity.PostShare;
import org.example.chat.entity.User;
import org.example.chat.repository.PostRepository;
import org.example.chat.repository.PostShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostShareService {

    private final PostRepository postRepository;
    private final PostShareRepository shareRepository;

    @Transactional
    public PostShare share(Long postId, User caller) {
        if (!postRepository.existsById(postId)) {
            throw new EntityNotFoundException("Post not found: " + postId);
        }
        return shareRepository.save(PostShare.builder()
                .postId(postId)
                .userId(caller.getId())
                .build());
    }
}
