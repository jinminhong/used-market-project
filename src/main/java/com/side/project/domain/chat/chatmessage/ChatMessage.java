package com.side.project.domain.chat.chatmessage;

import com.side.project.domain.chat.chatroom.ChatRoom;
import com.side.project.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.*;
import static jakarta.persistence.GenerationType.*;

@Entity
@NoArgsConstructor
@Getter
public class ChatMessage {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "chatMessage_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chatroom_id" ,nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private OfferStatus offerStatus;

    private Integer offeredPrice;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public ChatMessage(
            ChatRoom chatRoom,
            Member sender,
            String content,
            MessageType messageType) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.messageType = messageType;
    }

    public void offerChatMessage(ChatRoom chatRoom,
                                 Member sender,
                                 String content,
                                 MessageType messageType,
                                 int offeredPrice) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.messageType = messageType;
        this.offeredPrice = offeredPrice;
        this.offerStatus = OfferStatus.PENDING;
    }
}
