package org.example.chat.listener;

import lombok.RequiredArgsConstructor;
import org.example.chat.dto.PresenceEvent;
import org.example.chat.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    @Transactional
    public void handleConnect(SessionConnectEvent event) {
        Principal user = StompHeaderAccessor.wrap(event.getMessage()).getUser();
        setOnline(user, true);
    }

    @EventListener
    @Transactional
    public void handleDisconnect(SessionDisconnectEvent event) {
        setOnline(event.getUser(), false);
    }

    private void setOnline(Principal principal, boolean online) {
        if (principal == null) {
            return;
        }
        userRepository.findByUsername(principal.getName()).ifPresent(u -> {
            u.setOnline(online);
            if (!online) {
                u.setLastSeenAt(LocalDateTime.now());
            }
            userRepository.save(u);
            messagingTemplate.convertAndSend("/topic/presence",
                    PresenceEvent.builder()
                            .userId(u.getId())
                            .username(u.getUsername())
                            .online(online)
                            .lastSeenAt(u.getLastSeenAt())
                            .build());
        });
    }
}
