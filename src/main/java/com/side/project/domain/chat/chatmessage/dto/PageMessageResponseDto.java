package com.side.project.domain.chat.chatmessage.dto;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageMessageResponseDto {

    private List<ChatMessageDto> list = new ArrayList<>();

    private boolean hasNext;

    public PageMessageResponseDto(List<ChatMessageDto> list, boolean hasNext) {
        this.list = list;
        this.hasNext = hasNext;
    }
}
