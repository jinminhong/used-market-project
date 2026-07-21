package com.side.project.domain.chat.chatroom.repository;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.side.project.domain.chat.chatmessage.QChatMessage;
import com.side.project.domain.chat.chatroom.ChatRoom;
import com.side.project.domain.chat.chatroom.QChatRoom;
import com.side.project.domain.chat.chatroom.dto.ChatRoomDto;
import com.side.project.domain.chat.chatroom.dto.QChatRoomDto;
import com.side.project.domain.item.QItem;
import com.side.project.domain.member.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.ArrayList;
import java.util.List;

import static com.side.project.domain.chat.chatmessage.QChatMessage.*;
import static com.side.project.domain.chat.chatroom.QChatRoom.*;
import static com.side.project.domain.item.QItem.*;
import static com.side.project.domain.member.QMember.*;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ChatRoomDto> getChatRooms(Long memberId, Pageable pageable) {
        QChatMessage subMessage = new QChatMessage("subMessage");

        int pageSize = pageable.getPageSize();

        List<ChatRoomDto> chatRoomList = queryFactory.select(new QChatRoomDto(chatRoom.id,
                        item.id,item.name,item.thumbnailImage.storedFilename,chatMessage.content,
                        chatRoom.buyer.id, chatRoom.buyer.nickName ,item.seller.id,item.seller.nickName,
                        chatRoom.lastMessageAt))
                .from(chatRoom)
                .join(chatRoom.item, item)
                .join(chatRoom.item.seller, member)
                .join(chatRoom.buyer, member)
                .leftJoin(chatMessage)
                .on(chatMessage.chatRoom.eq(chatRoom)
                        .and(
                                chatMessage.id.eq(
                                        JPAExpressions
                                                .select(subMessage.id.max())
                                                .from(subMessage)
                                                .where(
                                                        subMessage.chatRoom.eq(chatRoom)
                                                )
                                )
                        )
                )
                .where(chatRoom.buyer.id.eq(memberId).or(chatRoom.item.seller.id.eq(memberId)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        boolean hasNext = chatRoomList.size() > pageSize;

        if (hasNext) {
            chatRoomList.remove(pageSize);
        }

        return new SliceImpl<>(chatRoomList, pageable, hasNext);

    }
}
