package org.example.chat.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chat.entity.Friendship;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendshipResponse {
    private Long id;
    private Long requesterId;
    private Long receiverId;
    private Friendship.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}