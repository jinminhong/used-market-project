package com.side.project.domain.chat.chatmessage;

import com.side.project.domain.chat.chatmessage.dto.ChatMessageRequest;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageResponse;
import com.side.project.domain.chat.chatmessage.repository.ChatMessageRepository;
import com.side.project.domain.chat.chatroom.ChatRoom;
import com.side.project.domain.chat.chatroom.dto.ChatRoomRequest;
import com.side.project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.web.exception.chat.message.ChatMessageException;
import com.side.project.web.exception.chat.room.ChatRoomException;
import com.side.project.web.exception.member.MemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long senderId, ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.chatRoomFetchJoinItem(roomId).orElseThrow(() -> new ChatRoomException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));
        if (!chatRoom.containsMember(senderId)) {
            throw new ChatRoomException(HttpStatus.FORBIDDEN,"해당 채팅방에 참여할 수 없습니다.");
        }
        String message = request.content().trim();
        if (message.isBlank()) {
            throw new ChatMessageException("메세지 내용을 입력해야 합니다.");
        }
        Member sender = memberRepository.findById(senderId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        ChatMessage chatMessage = new ChatMessage(chatRoom, sender, message , MessageType.TEXT);
        chatMessageRepository.save(chatMessage);
        chatRoom.updateLastMessageAt(chatMessage.getSentAt());
        return ChatMessageResponse.from(chatMessage);
    }

    @Transactional
    public ChatMessage sendOffer(ChatRoom chatRoom, Member buyer , ChatRoomRequest request) {
        String message = request.content().trim();
        if (message.isBlank()) {
            throw new ChatMessageException("메세지 내용을 입력해야 합니다.");
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.offerChatMessage(chatRoom,buyer,message,MessageType.OFFER, request.offeredPrice());
        chatMessageRepository.save(chatMessage);
        chatRoom.updateLastMessageAt(chatMessage.getSentAt());
        return chatMessage;
    }

    @Transactional
    public ChatMessage rejectOffer(ChatRoom chatRoom , Member seller , Long messageId) {
        ChatMessage offerChatMessage = getChatMessageById(messageId);
        offerChatMessage.changeStatusToReject();

        String message = "거절합니다.";
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.rejectOfferMessage(chatRoom,seller,message,MessageType.OFFER , offerChatMessage.getOfferedPrice());
        chatMessageRepository.save(chatMessage);

        return chatMessage;
    }

    @Transactional
    public ChatMessage acceptOffer(ChatRoom chatRoom, Member seller, ChatMessage offerChatMessage) {
        offerChatMessage.changeStatusToAccept();

        String message = "제안을 수락합니다.";
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.acceptOfferMessage(chatRoom,seller,message,MessageType.OFFER , offerChatMessage.getOfferedPrice());
        chatMessageRepository.save(chatMessage);
        chatRoom.updateLastMessageAt(chatMessage.getSentAt());

        Long itemId = chatRoom.getItem().getId();
        chatMessageRepository.rejectOtherOffers(itemId,offerChatMessage.getId());

        return chatMessage;
    }

    public ChatMessage getChatMessageById(Long messageId) {
        return chatMessageRepository.findById(messageId).orElseThrow(() -> new ChatMessageException("메세지를 찾을 수 없습니다."));
    }
}
