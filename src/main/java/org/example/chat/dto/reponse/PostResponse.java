package org.example.chat.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {
    private Long id;
    private Long userId;
    private String content;
    private String mediaUrl;
    private LocalDateTime createdAt;
    private Boolean isOwner;
    private long commentCount;
    private long shareCount;
    private Map<String, Long> reactionSummary;
    private List<CommentDTO> comments;
}
