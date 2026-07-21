package com.side.project.domain.chat.chatroom;

import com.side.project.domain.BaseEntity;
import com.side.project.domain.item.Item;
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
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_item_buyer",
                        columnNames = {"item_id", "buyer_id"}
                )
        }
)
public class ChatRoom{

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "chatroom_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "buyer_id" , nullable = false)
    private Member buyer;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "item_id" ,  nullable = false)
    private Item item;


    private LocalDateTime createdAt;

    private LocalDateTime lastMessageAt;

    public ChatRoom(Item item, Member buyer) {
        this.item = item;
        this.buyer = buyer;
        this.createdAt = LocalDateTime.now();
        this.lastMessageAt = createdAt;
    }

    public boolean containsMember(Long memberId) {
        Long buyerId = buyer.getId();
        Long sellerId = item.getSeller().getId();

        return buyerId.equals(memberId) || sellerId.equals(memberId);
    }

    public void updateLastMessageAt(LocalDateTime sentAt) {
        this.lastMessageAt = sentAt;
    }
}
