package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private Long id;
    private String content;
    private Long senderId;
    private String senderName;
    private Long roomId;
    private LocalDateTime createdAt;
    private boolean edited;
    private String attachmentUrl;
    private String attachmentType;
    private String attachmentName;
    private List<ReactionSummary> reactions;
}
