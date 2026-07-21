package com.side.project.domain.chat.chatroom.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class ChatRoomDto {

    private Long roomId;
    private Long itemId;
    private String itemName;
    private String storedFilename;
    @Setter
    private String counterPartNickName;
    private String lastMessage;
    private Long buyerId;
    private Long sellerId;
    private String buyerNickName;
    private String sellerNickName;
    private LocalDateTime LastMessageAt;

    @QueryProjection
    public ChatRoomDto(Long roomId, Long itemId, String itemName,
                       String storedFilename, String lastMessage, Long buyerId,
                       String buyerNickName, Long sellerId, String sellerNickName,
                       LocalDateTime lastMessageAt) {
        this.roomId = roomId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.storedFilename = storedFilename;
        this.lastMessage = lastMessage;
        this.buyerId = buyerId;
        this.buyerNickName = buyerNickName;
        this.sellerId = sellerId;
        this.sellerNickName = sellerNickName;
        LastMessageAt = lastMessageAt;
    }

}
