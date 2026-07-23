package com.side.project.domain.chat.chatroom;

import com.side.project.domain.chat.chatmessage.ChatMessage;
import com.side.project.domain.chat.chatmessage.ChatMessageService;
import com.side.project.domain.chat.chatmessage.MessageType;
import com.side.project.domain.chat.chatmessage.OfferStatus;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageDto;
import com.side.project.domain.chat.chatmessage.dto.PageMessageResponseDto;
import com.side.project.domain.chat.chatmessage.repository.ChatMessageRepository;
import com.side.project.domain.chat.chatroom.dto.*;
import com.side.project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemService;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.Orders;
import com.side.project.domain.orders.OrdersService;
import com.side.project.domain.orders.repository.OrdersRepository;
import com.side.project.web.exception.chat.message.ChatMessageException;
import com.side.project.web.exception.chat.room.ChatRoomException;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.login.UnauthorizedException;
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
    private final ChatMessageService chatMessageService;
    private final OrdersService ordersService;
    private final ItemService itemService;

    @Transactional
    public ChatRoomResponse createChatRoom(Long itemId , Long buyerId) {
        Item item = itemRepository.findByIdWithMember(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다."));

        Long sellerId = item.getSeller().getId();
        if (sellerId.equals(buyerId)) {
            throw new ChatRoomException(HttpStatus.CONFLICT,"본인이 등록한 상품에는 문의할 수 없습니다.");
        }

        Member buyer = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        return chatRoomRepository.findChatRoomByItemAndBuyer(itemId, buyerId)
                .map(ChatRoomResponse::from)
                .orElseGet(() -> {
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
        chatRoomContainMember(roomId, memberId);

        Slice<ChatMessageDto> messages = chatMessageRepository.getMessages(roomId, pageable);

        return new PageMessageResponseDto(messages.getContent(), messages.hasNext());
    }

    @Transactional
    public ChatRoomAndMessageDto createOffer(Long itemId, Long buyerId, ChatRoomRequest request) {
        Item item = itemRepository.findByIdWithMember(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다"));

        Long sellerId = item.getSeller().getId();
        if (sellerId.equals(buyerId)) {
            throw new ChatRoomException(HttpStatus.CONFLICT,"본인이 등록한 상품에는 문의할 수 없습니다.");
        }

        Member buyer = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findChatRoomByItemAndBuyer(itemId, buyerId)
                .orElseGet(() -> {
                    ChatRoom chatRoom1 = new ChatRoom(item, buyer);
                    chatRoomRepository.save(chatRoom1);
                    return chatRoom1;
                });

        ChatMessage chatMessage = chatMessageService.sendOffer(chatRoom, buyer, request);
        return ChatRoomAndMessageDto.from(chatRoom, chatMessage);
    }

    @Transactional
    public ChatRoomAndMessageDto rejectOffer(Long roomId, Long memberId, Long messageId) {
        ChatRoom chatRoom = chatRoomContainMember(roomId, memberId);

        Item item = chatRoom.getItem();
        if (!item.getSeller().getId().equals(memberId)) {
            throw new UnauthorizedException("제안을 거절할 권한이 없습니다.");
        }

        if (item.getStatus() != ItemStatus.SELLING) {
            throw new ItemException("이미 거래 중이거나 완료된 상품입니다.");
        }

        Member seller = memberRepository.findById(memberId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        ChatMessage chatMessage = chatMessageService.rejectOffer(chatRoom, seller, messageId);

        chatRoom.updateLastMessageAt(chatMessage.getSentAt());
        return ChatRoomAndMessageDto.from(chatRoom, chatMessage);
    }

    @Transactional
    public ChatRoomAndMessageDto acceptOffer(Long roomId, Long seller, Long messageId) {
        ChatRoom chatRoom = chatRoomContainMember(roomId, seller);

        Item item = chatRoom.getItem();
        if (!item.getSeller().getId().equals(seller)) {
            throw new UnauthorizedException("제안을 수락할 권한이 없습니다.");
        }
        Member findSeller = memberRepository.findById(seller).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        ChatMessage offeredMessage = chatMessageService.getChatMessageById(messageId);
        if (!offeredMessage.getChatRoom().getId().equals(roomId) || offeredMessage.getMessageType() != MessageType.OFFER) {
            throw new ChatMessageException("해당 채팅방의 제안이 아닙니다.");
        }
        if (offeredMessage.getOfferStatus() != OfferStatus.PENDING) {
            throw new ChatMessageException("이미 처리된 제안입니다.");
        }
        if (item.getStatus() != ItemStatus.SELLING) {
            throw new ItemException("이미 거래 중이거나 완료된 상품입니다.");
        }

        Integer offeredPrice = offeredMessage.getOfferedPrice();
        itemService.reserveForOffer(item);

        Long orderId = ordersService.createOrders(offeredMessage.getSender(), item, OrderStatus.ACCEPTED, offeredPrice);

        ChatMessage chatMessage = chatMessageService.acceptOffer(chatRoom, findSeller, offeredMessage);
        return ChatRoomAndMessageDto.fromAccepted(chatRoom, chatMessage, orderId);
    }

    private ChatRoom chatRoomContainMember(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.chatRoomFetchJoinItem(roomId).orElseThrow(() -> new ChatRoomException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        if (!chatRoom.containsMember(memberId)) {
            throw new ChatRoomException(HttpStatus.FORBIDDEN, "해당 채팅방에 참여할 수 없습니다.");
        }
        return chatRoom;
    }
}
