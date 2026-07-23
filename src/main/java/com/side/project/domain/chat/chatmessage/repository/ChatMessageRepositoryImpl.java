package com.side.project.domain.chat.chatmessage.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.side.project.domain.chat.chatmessage.QChatMessage;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageDto;
import com.side.project.domain.chat.chatmessage.dto.QChatMessageDto;
import com.side.project.domain.chat.chatroom.QChatRoom;
import com.side.project.domain.member.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.ArrayList;
import java.util.List;

import static com.side.project.domain.chat.chatmessage.QChatMessage.*;
import static com.side.project.domain.chat.chatroom.QChatRoom.*;
import static com.side.project.domain.member.QMember.*;

@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ChatMessageDto> getMessages(Long roomId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<ChatMessageDto> chatMessages = queryFactory.select(new QChatMessageDto(chatMessage.id,
                        chatRoom.item.id, member.id, chatMessage.sentAt, member.nickName, chatMessage.content,
                        chatMessage.messageType, chatMessage.offeredPrice, chatMessage.offerStatus))
                .from(chatMessage)
                .join(chatMessage.sender, member)
                .join(chatMessage.chatRoom, chatRoom)
                .where(chatRoom.id.eq(roomId))
                .orderBy(chatMessage.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = chatMessages.size() > pageSize;

        if (hasNext) {
            chatMessages.remove(pageSize);
        }

        return new SliceImpl<>(chatMessages, pageable, hasNext);
    }
}
