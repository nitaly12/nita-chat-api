package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReactionEvent {

    public enum Action { ADDED, REMOVED, REPLACED }

    private Action action;
    private Long messageId;
    private Long roomId;
    private String emoji;
    private Long userId;
    private String username;
    private Map<String, Long> reactionSummary;
}
