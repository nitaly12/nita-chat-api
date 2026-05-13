package org.example.chat.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chat.entity.Friendship;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendDTO {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String friendshipStatus;

    public FriendDTO(Long id, String username, String displayName, String avatarUrl,
                     Friendship.Status status) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.friendshipStatus = (status != null) ? status.name() : null;
    }
}