package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberEvent {

    public enum Type { JOINED, LEFT, REMOVED }

    private Type type;
    private Long roomId;
    private Long userId;
    private String username;
}
