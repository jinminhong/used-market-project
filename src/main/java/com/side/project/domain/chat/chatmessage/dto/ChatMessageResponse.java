package com.side.project.domain.chat.chatmessage.dto;

import com.side.project.domain.chat.chatmessage.ChatMessage;
import com.side.project.domain.chat.chatmessage.MessageType;
import com.side.project.domain.chat.chatmessage.OfferStatus;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        MessageType messageType,
        Integer offeredPrice,
        OfferStatus offerStatus,
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
                message.getMessageType(),
                message.getOfferedPrice(),
                message.getOfferStatus(),
                message.getSentAt()
        );
    }
}
