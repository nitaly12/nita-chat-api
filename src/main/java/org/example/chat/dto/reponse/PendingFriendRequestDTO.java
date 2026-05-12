package org.example.chat.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PendingFriendRequestDTO {
    private Long id;
    private Long requesterId;
    private String requesterUsername;
    private String requesterDisplayName;
    private String requesterAvatarUrl;
    private LocalDateTime createdAt;
}