package org.example.chat.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicProfileResponse {
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
}
