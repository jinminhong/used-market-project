package com.side.project.domain.chat.chatmessage;

import com.side.project.domain.chat.chatmessage.dto.ChatMessageRequest;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageResponse;
import com.side.project.web.SessionConst;
import com.side.project.web.exception.chat.message.ChatMessageException;
import com.side.project.web.exception.chat.room.ChatRoomException;
import com.side.project.web.login.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat/rooms/{chatRoomId}/messages")
    public void sendMessage(@DestinationVariable Long chatRoomId,
                            @Valid ChatMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> sessionAttributes =
                headerAccessor.getSessionAttributes();

        if (sessionAttributes == null) {
            throw new ChatMessageException("로그인이 필요합니다.");
        }

        LoginMember loginMember =
                (LoginMember) sessionAttributes.get(
                        SessionConst.LOGIN_MEMBER
                );

        if (loginMember == null) {
            throw new ChatMessageException("로그인이 필요합니다.");
        }

        ChatMessageResponse response = chatMessageService.sendMessage(chatRoomId,loginMember.getMemberId(),request);

        messagingTemplate.convertAndSend(
                "/topic/chat/rooms/" + chatRoomId,
                response
        );
    }

    @MessageExceptionHandler(ChatMessageException.class)
    public void handleChatMessageException(ChatMessageException e) {
        log.warn("STOMP 채팅 메시지 처리 실패: {}", e.getMessage());
    }

    @MessageExceptionHandler(ChatRoomException.class)
    public void handleChatRoomException(ChatRoomException e) {
        log.warn("STOMP 채팅방 처리 실패: {}", e.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationException(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse(e.getMessage());
        log.warn("STOMP 채팅 메시지 검증 실패: {}", detail);
    }
}
