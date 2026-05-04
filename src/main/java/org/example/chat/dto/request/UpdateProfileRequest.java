package org.example.chat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 80)
    private String displayName;

    @Size(max = 500)
    private String bio;

    @Size(max = 1024)
    private String avatarUrl;
}
