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
public class ChatNotificationDTO {
    private Long chatId;
    private String senderName;
    private String senderAvatar;
    private String contentPreview;
    private LocalDateTime sentAt;
}
