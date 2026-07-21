package com.side.project.domain.chat.chatroom.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageChatRoomResponse {

    List<ChatRoomDto> chatRooms = new ArrayList<>();

    Boolean hasNext;

    public PageChatRoomResponse(List<ChatRoomDto> chatRooms, Boolean hasNext) {
        this.chatRooms = chatRooms;
        this.hasNext = hasNext;
    }
}
