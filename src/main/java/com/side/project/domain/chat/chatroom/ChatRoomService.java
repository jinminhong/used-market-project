package com.side.project.domain.chat.chatroom;

import com.side.project.domain.chat.chatmessage.dto.ChatMessageDto;
import com.side.project.domain.chat.chatmessage.dto.PageMessageResponseDto;
import com.side.project.domain.chat.chatmessage.repository.ChatMessageRepository;
import com.side.project.domain.chat.chatroom.dto.ChatRoomDto;
import com.side.project.domain.chat.chatroom.dto.ChatRoomResponse;
import com.side.project.domain.chat.chatroom.dto.PageChatRoomResponse;
import com.side.project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.web.exception.chat.room.ChatRoomException;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.member.MemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;


    @Transactional
    public ChatRoomResponse createChatRoom(Long itemId , Long buyerId) {
        Item item = itemRepository.findByIdWithMember(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다."));

        Long sellerId = item.getSeller().getId();
        if (sellerId.equals(buyerId)) {
            throw new ChatRoomException(HttpStatus.CONFLICT,"본인이 등록한 상품에는 문의할 수 없습니다.");
        }

        return chatRoomRepository.findChatRoomByItemAndBuyer(itemId, buyerId)
                .map(ChatRoomResponse::from)
                .orElseGet(() -> {
                    Member buyer = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
                    ChatRoom chatRoom = new ChatRoom(item, buyer);
                    chatRoomRepository.save(chatRoom);
                    return ChatRoomResponse.from(chatRoom);
                });
    }

    public PageChatRoomResponse getRooms(Long memberId , Pageable pageable) {
        Slice<ChatRoomDto> chatRooms = chatRoomRepository.getChatRooms(memberId, pageable);
        List<ChatRoomDto> content = chatRooms.getContent();
        for (ChatRoomDto chatRoomDto : content) {
            if (chatRoomDto.getBuyerId().equals(memberId)) {
                chatRoomDto.setCounterPartNickName(chatRoomDto.getSellerNickName());
            } else {
                chatRoomDto.setCounterPartNickName(chatRoomDto.getBuyerNickName());
            }
        }
        return new PageChatRoomResponse(content, chatRooms.hasNext());
    }

    public PageMessageResponseDto getMessages(Long roomId, Long memberId, Pageable pageable) {
        ChatRoom chatRoom = chatRoomRepository.chatRoomFetchJoinItem(roomId).orElseThrow(() -> new ChatRoomException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        if (!chatRoom.containsMember(memberId)) {
            throw new ChatRoomException(HttpStatus.FORBIDDEN, "해당 채팅방에 참여할 수 없습니다.");
        }

        Slice<ChatMessageDto> messages = chatMessageRepository.getMessages(roomId, pageable);

        return new PageMessageResponseDto(messages.getContent(), messages.hasNext());
    }

}
