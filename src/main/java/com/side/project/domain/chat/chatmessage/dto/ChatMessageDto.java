package com.side.project.domain.chat.chatmessage.dto;

import com.querydsl.core.annotations.QueryProjection;
import com.side.project.domain.chat.chatmessage.MessageType;
import com.side.project.domain.chat.chatmessage.OfferStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageDto {

    private Long messageId;
    private Long itemId;
    private Long senderId;
    private LocalDateTime sendAt;
    private String senderNickname;
    private String content;
    private MessageType messageType;
    private Integer offeredPrice;
    private OfferStatus offerStatus;
    private Long orderId;

    @QueryProjection
    public ChatMessageDto(Long messageId, Long itemId, Long senderId, LocalDateTime sendAt, String senderNickname, String content,
                          MessageType messageType, Integer offeredPrice, OfferStatus offerStatus, Long orderId) {
        this.messageId = messageId;
        this.itemId = itemId;
        this.senderId = senderId;
        this.sendAt = sendAt;
        this.senderNickname = senderNickname;
        this.content = content;
        this.messageType = messageType;
        this.offeredPrice = offeredPrice;
        this.offerStatus = offerStatus;
        this.orderId = orderId;
    }

}
