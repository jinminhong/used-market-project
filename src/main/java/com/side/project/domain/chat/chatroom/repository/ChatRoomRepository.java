package com.side.project.domain.chat.chatroom.repository;

import com.side.project.domain.chat.chatroom.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> , ChatRoomRepositoryCustom{

    @Query("select cr from ChatRoom cr join fetch cr.item i where cr.id=:roomId ")
    Optional<ChatRoom> chatRoomFetchJoinItem(@Param("roomId")Long roomId);

    @Query("select cr from ChatRoom cr join fetch cr.item i where i.id=:itemId and cr.buyer.id=:buyerId")
    Optional<ChatRoom> findChatRoomByItemAndBuyer(@Param("itemId") Long itemId, @Param("buyerId") Long buyerId);

}
