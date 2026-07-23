package com.side.project.domain.chat.chatroom.dto;

import com.side.project.domain.chat.chatmessage.ChatMessage;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageResponse;
import com.side.project.domain.chat.chatroom.ChatRoom;

public record ChatRoomAndMessageDto(
        ChatRoomResponse room,
        ChatMessageResponse message
) {
    public static ChatRoomAndMessageDto from(ChatRoom chatRoom, ChatMessage message) {
        return new ChatRoomAndMessageDto(
                ChatRoomResponse.from(chatRoom),
                ChatMessageResponse.from(message)
        );
    }
}
