package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceiptEvent {
    private Long roomId;
    private Long userId;
    private String username;
    private Long lastReadMessageId;
    private LocalDateTime readAt;
}
