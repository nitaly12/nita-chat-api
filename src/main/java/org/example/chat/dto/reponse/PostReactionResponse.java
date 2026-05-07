package org.example.chat.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostReactionResponse {

    public enum Action { ADDED, REMOVED }

    private Long postId;
    private String emoji;
    private Action action;
    private Map<String, Long> reactionSummary;
}
