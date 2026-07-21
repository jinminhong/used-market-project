package com.side.project.domain.chat.chatroom.dto;

import com.side.project.domain.chat.chatroom.ChatRoom;

public record ChatRoomResponse(
        Long roomId,
        Long itemId,
        Long buyerId,
        Long sellerId,
        String itemName,
        String sellerNicName,
        String buyerNickName
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getItem().getId(),
                chatRoom.getBuyer().getId(),
                chatRoom.getItem().getSeller().getId(),
                chatRoom.getItem().getName(),
                chatRoom.getItem().getSeller().getNickName(),
                chatRoom.getBuyer().getNickName()
        );
    }
}
