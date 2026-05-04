package org.example.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageEditRequest {

    @NotBlank
    @Size(max = 4000)
    private String content;
}
