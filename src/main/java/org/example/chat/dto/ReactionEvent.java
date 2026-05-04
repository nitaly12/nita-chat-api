package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReactionEvent {

    public enum Action { ADDED, REMOVED }

    private Action action;
    private Long messageId;
    private Long roomId;
    private String emoji;
    private Long userId;
    private String username;
}
