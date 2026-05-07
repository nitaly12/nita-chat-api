package org.example.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "content must not be blank")
    @Size(max = 5000, message = "content must be at most 5000 characters")
    private String content;

    @Size(max = 1024, message = "mediaUrl must be at most 1024 characters")
    private String mediaUrl;
}
