package com.side.project.domain.chat.chatmessage.repository;

import com.side.project.domain.chat.chatmessage.dto.ChatMessageDto;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ChatMessageRepositoryCustom {

    Slice<ChatMessageDto> getMessages(Long roomId , Pageable pageable);
}
