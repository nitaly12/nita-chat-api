package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Message parentMessage;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "attachment_url", length = 1024)
    private String attachmentUrl;

    @Column(name = "attachment_type", length = 128)
    private String attachmentType;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false, columnDefinition = "boolean NOT NULL DEFAULT false")
    @Builder.Default
    private boolean edited = false;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
