package com.side.project.domain.chat.chatmessage;

import com.side.project.domain.chat.chatmessage.dto.ChatMessageRequest;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageResponse;
import com.side.project.domain.chat.chatmessage.repository.ChatMessageRepository;
import com.side.project.domain.chat.chatroom.ChatRoom;
import com.side.project.domain.chat.chatroom.dto.ChatRoomRequest;
import com.side.project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.config.TestcontainersConfig;
import com.side.project.web.exception.chat.message.ChatMessageException;
import com.side.project.web.exception.chat.room.ChatRoomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfig.class)
@SpringBootTest
@Transactional
class ChatMessageServiceTest {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member createAndSaveMember(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member(prefix + suffix, "이름", "password123", prefix + "Nick" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호"));
        return memberRepository.save(member);
    }

    private Item createAndSaveItem(Member seller) {
        Item item = new Item("상품명" + UUID.randomUUID(), "설명", 10000, ItemStatus.SELLING, Category.TOP, seller);
        item.addItemImage(new ItemImage("original.png", "stored.png"));
        return itemRepository.save(item);
    }

    private ChatRoom createAndSaveChatRoom(Item item, Member buyer) {
        return chatRoomRepository.save(new ChatRoom(item, buyer));
    }

    @Test
    void sendMessage_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        ChatRoom chatRoom = createAndSaveChatRoom(item, buyer);
        ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

        ChatMessageResponse response = chatMessageService.sendMessage(chatRoom.getId(), buyer.getId(), request);

        assertThat(response.content()).isEqualTo("안녕하세요");
        assertThat(response.messageType()).isEqualTo(MessageType.TEXT);
        assertThat(chatMessageRepository.findById(response.messageId())).isPresent();
    }

    @Test
    void sendMessage_방없음() {
        Member buyer = createAndSaveMember("buyer");
        ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

        assertThatThrownBy(() -> chatMessageService.sendMessage(-1L, buyer.getId(), request))
                .isInstanceOf(ChatRoomException.class)
                .extracting(e -> ((ChatRoomException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void sendMessage_미참여자() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Member other = createAndSaveMember("other");
        Item item = createAndSaveItem(seller);
        ChatRoom chatRoom = createAndSaveChatRoom(item, buyer);
        ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

        assertThatThrownBy(() -> chatMessageService.sendMessage(chatRoom.getId(), other.getId(), request))
                .isInstanceOf(ChatRoomException.class)
                .extracting(e -> ((ChatRoomException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sendMessage_빈메시지() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        ChatRoom chatRoom = createAndSaveChatRoom(item, buyer);
        ChatMessageRequest request = new ChatMessageRequest("   ");

        assertThatThrownBy(() -> chatMessageService.sendMessage(chatRoom.getId(), buyer.getId(), request))
                .isInstanceOf(ChatMessageException.class);
    }

    @Test
    void sendOffer_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        ChatRoom chatRoom = createAndSaveChatRoom(item, buyer);
        ChatRoomRequest request = new ChatRoomRequest(item.getId(), "가격 제안합니다", 8000);

        ChatMessage result = chatMessageService.sendOffer(chatRoom, buyer, request);

        assertThat(result.getMessageType()).isEqualTo(MessageType.OFFER);
        assertThat(result.getOfferStatus()).isEqualTo(OfferStatus.PENDING);
        assertThat(result.getOfferedPrice()).isEqualTo(8000);
    }

    @Test
    void sendOffer_빈메시지() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        ChatRoom chatRoom = createAndSaveChatRoom(item, buyer);
        ChatRoomRequest request = new ChatRoomRequest(item.getId(), "   ", 8000);

        assertThatThrownBy(() -> chatMessageService.sendOffer(chatRoom, buyer, request))
                .isInstanceOf(ChatMessageException.class);
    }

    @Test
    void rejectOffer_상태전이() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        ChatRoom chatRoom = createAndSaveChatRoom(item, buyer);
        ChatMessage offerMessage = new ChatMessage();
        offerMessage.offerChatMessage(chatRoom, buyer, "가격 제안합니다", MessageType.OFFER, 8000);
        chatMessageRepository.save(offerMessage);

        ChatMessage result = chatMessageService.rejectOffer(chatRoom, seller, offerMessage, 10L);

        assertThat(offerMessage.getOfferStatus()).isEqualTo(OfferStatus.REJECTED);
        assertThat(result.getOfferStatus()).isEqualTo(OfferStatus.REJECTED);
        assertThat(result.getOrderId()).isEqualTo(10L);
    }

    @Test
    void acceptOffer_상태전이_및_다른제안거절() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        ChatRoom chatRoom = createAndSaveChatRoom(item, buyer);
        ChatMessage offerMessage = new ChatMessage();
        offerMessage.offerChatMessage(chatRoom, buyer, "가격 제안합니다", MessageType.OFFER, 8000);
        chatMessageRepository.save(offerMessage);

        ChatMessage result = chatMessageService.acceptOffer(chatRoom, seller, offerMessage, 10L);

        assertThat(offerMessage.getOfferStatus()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(result.getOfferStatus()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(result.getOrderId()).isEqualTo(10L);
    }

    @Test
    void getChatMessageById_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        ChatRoom chatRoom = createAndSaveChatRoom(item, buyer);
        ChatMessage message = chatMessageRepository.save(new ChatMessage(chatRoom, buyer, "안녕하세요", MessageType.TEXT));

        ChatMessage result = chatMessageService.getChatMessageById(message.getId());

        assertThat(result.getId()).isEqualTo(message.getId());
    }

    @Test
    void getChatMessageById_존재하지않음() {
        assertThatThrownBy(() -> chatMessageService.getChatMessageById(-1L))
                .isInstanceOf(ChatMessageException.class);
    }
}
