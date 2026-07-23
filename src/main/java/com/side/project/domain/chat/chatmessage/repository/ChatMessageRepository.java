package com.side.project.domain.chat.chatmessage.repository;

import com.side.project.domain.chat.chatmessage.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> , ChatMessageRepositoryCustom {


    @Modifying(
            flushAutomatically = true
    )
    @Query(value = """
        UPDATE chat_message AS cm
        SET offer_status = 'REJECTED'
        WHERE cm.message_type = 'OFFER'
          AND cm.offer_status = 'PENDING'
          AND cm.chat_message_id <> :acceptedMessageId
          AND EXISTS (
              SELECT 1
              FROM chat_room AS cr
              WHERE cr.chatroom_id = cm.chatroom_id
                AND cr.item_id = :itemId
          )
        """, nativeQuery = true)
    int rejectOtherOffers(@Param("itemId") Long itemId, @Param("acceptedMessageId") Long acceptedMessageId);

}
