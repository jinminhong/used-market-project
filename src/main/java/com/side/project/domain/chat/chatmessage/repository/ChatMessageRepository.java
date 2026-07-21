package com.side.project.domain.chat.chatmessage.repository;

import com.side.project.domain.chat.chatmessage.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> , ChatMessageRepositoryCustom {



}
