package com.side.project.domain.chat.chatmessage;

import com.side.project.domain.chat.chatmessage.dto.ChatMessageRequest;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageResponse;
import com.side.project.web.SessionConst;
import com.side.project.web.exception.chat.message.ChatMessageException;
import com.side.project.web.login.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

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
}
