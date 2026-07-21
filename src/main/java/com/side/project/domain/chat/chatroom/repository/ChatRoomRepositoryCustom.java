package com.side.project.domain.chat.chatroom.repository;

import com.side.project.domain.chat.chatroom.ChatRoom;
import com.side.project.domain.chat.chatroom.dto.ChatRoomDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ChatRoomRepositoryCustom {
    Slice<ChatRoomDto> getChatRooms(Long memberId, Pageable pageable);
}
