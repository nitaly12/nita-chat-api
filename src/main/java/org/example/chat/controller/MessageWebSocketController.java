package org.example.chat.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.chat.dto.TypingEvent;
import org.example.chat.dto.request.MessageRequest;
import org.example.chat.dto.request.TypingRequest;
import org.example.chat.entity.User;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @MessageMapping("/chat.send/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload MessageRequest request,
                            Principal principal) {
        request.setRoomId(roomId);
        User sender = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + principal.getName()));
        messageService.sendMessage(request, sender);
    }

    @MessageMapping("/chat.typing/{roomId}")
    @SendTo("/topic/room/{roomId}/typing")
    public TypingEvent typing(@DestinationVariable Long roomId,
                              @Payload TypingRequest request,
                              Principal principal) {
        return TypingEvent.builder()
                .roomId(roomId)
                .username(principal.getName())
                .typing(request.isTyping())
                .build();
    }
}
