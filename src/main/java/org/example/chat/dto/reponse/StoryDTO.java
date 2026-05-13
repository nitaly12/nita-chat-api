package org.example.chat.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoryDTO {
    private Long id;
    private Long userId;
    private String displayName;
    private String userAvatarUrl;
    private String mediaUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
}