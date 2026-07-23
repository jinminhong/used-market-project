package com.side.project.domain.chat.chatmessage.dto;

import com.querydsl.core.annotations.QueryProjection;
import com.side.project.domain.chat.chatmessage.MessageType;
import com.side.project.domain.chat.chatmessage.OfferStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageDto {

    private Long roomId;
    private Long itemId;
    private Long senderId;
    private LocalDateTime sendAt;
    private String senderNickname;
    private String content;
    private MessageType messageType;
    private Integer offeredPrice;
    private OfferStatus offerStatus;

    @QueryProjection
    public ChatMessageDto(Long roomId, Long itemId, Long senderId, LocalDateTime sendAt, String senderNickname, String content,
                          MessageType messageType, Integer offeredPrice, OfferStatus offerStatus) {
        this.roomId = roomId;
        this.itemId = itemId;
        this.senderId = senderId;
        this.sendAt = sendAt;
        this.senderNickname = senderNickname;
        this.content = content;
        this.messageType = messageType;
        this.offeredPrice = offeredPrice;
        this.offerStatus = offerStatus;
    }

}
