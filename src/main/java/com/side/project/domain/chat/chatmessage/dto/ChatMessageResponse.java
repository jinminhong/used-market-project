package com.side.project.domain.chat.chatmessage.dto;

import com.side.project.domain.chat.chatmessage.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime sentAt
) {
    public static ChatMessageResponse from(
            ChatMessage message
    ) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getNickName(),
                message.getContent(),
                message.getSentAt()
        );
    }
}
