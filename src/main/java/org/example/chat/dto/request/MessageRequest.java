package org.example.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest {

    @NotNull
    private Long roomId;

    @NotNull
    @Size(max = 4000)
    private String content;

    @Size(max = 1024)
    private String attachmentUrl;

    @Size(max = 128)
    private String attachmentType;

    @Size(max = 255)
    private String attachmentName;
}
