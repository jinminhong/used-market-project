package com.side.project.domain.chat.chatroom;

import com.side.project.domain.chat.chatmessage.ChatMessage;
import com.side.project.domain.chat.chatmessage.OfferStatus;
import com.side.project.domain.chat.chatmessage.repository.ChatMessageRepository;
import com.side.project.domain.chat.chatroom.dto.ChatRoomAndMessageDto;
import com.side.project.domain.chat.chatroom.dto.ChatRoomRequest;
import com.side.project.domain.chat.chatroom.dto.ChatRoomResponse;
import com.side.project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.Orders;
import com.side.project.domain.orders.repository.OrdersRepository;
import com.side.project.web.exception.chat.message.ChatMessageException;
import com.side.project.web.exception.chat.room.ChatRoomException;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.login.UnauthorizedException;
import com.side.project.web.exception.member.MemberException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ChatRoomServiceTest {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    private Member createAndSaveMember(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member(prefix + suffix, "이름", "password123", prefix + "Nick" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호"));
        return memberRepository.save(member);
    }

    private Item createAndSaveItem(Member seller, ItemStatus status) {
        Item item = new Item("상품명" + UUID.randomUUID(), "설명", 10000, status, Category.TOP, seller);
        item.addItemImage(new ItemImage("original.png", "stored.png"));
        return itemRepository.save(item);
    }

    @Test
    void createChatRoom_신규생성() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        ChatRoomResponse response = chatRoomService.createChatRoom(item.getId(), buyer.getId());

        assertThat(response.itemId()).isEqualTo(item.getId());
        assertThat(response.buyerId()).isEqualTo(buyer.getId());
        assertThat(chatRoomRepository.findChatRoomByItemAndBuyer(item.getId(), buyer.getId())).isPresent();
    }

    @Test
    void createChatRoom_기존방재사용() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoom existingRoom = chatRoomRepository.save(new ChatRoom(item, buyer));

        ChatRoomResponse response = chatRoomService.createChatRoom(item.getId(), buyer.getId());

        assertThat(response.roomId()).isEqualTo(existingRoom.getId());
    }

    @Test
    void createChatRoom_상품없음() {
        Member buyer = createAndSaveMember("buyer");

        assertThatThrownBy(() -> chatRoomService.createChatRoom(-1L, buyer.getId()))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getHttpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createChatRoom_본인상품() {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        assertThatThrownBy(() -> chatRoomService.createChatRoom(item.getId(), seller.getId()))
                .isInstanceOf(ChatRoomException.class)
                .extracting(e -> ((ChatRoomException) e).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createChatRoom_회원없음() {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        assertThatThrownBy(() -> chatRoomService.createChatRoom(item.getId(), -1L))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void getMessages_방없음() {
        assertThatThrownBy(() -> chatRoomService.getMessages(-1L, 1L, null))
                .isInstanceOf(ChatRoomException.class)
                .extracting(e -> ((ChatRoomException) e).getHttpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getMessages_미참여자() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Member other = createAndSaveMember("other");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(item, buyer));

        assertThatThrownBy(() -> chatRoomService.getMessages(chatRoom.getId(), other.getId(), null))
                .isInstanceOf(ChatRoomException.class)
                .extracting(e -> ((ChatRoomException) e).getHttpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createOffer_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoomRequest request = new ChatRoomRequest(item.getId(), "가격 제안합니다", 8000);

        ChatRoomAndMessageDto result = chatRoomService.createOffer(item.getId(), buyer.getId(), request);

        assertThat(result.message().offerStatus()).isEqualTo(OfferStatus.PENDING);
        Orders order = ordersRepository.findById(result.message().orderId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(order.getAgreedPrice()).isEqualTo(8000);
    }

    @Test
    void createOffer_상품없음() {
        Member buyer = createAndSaveMember("buyer");
        ChatRoomRequest request = new ChatRoomRequest(-1L, "가격 제안합니다", 8000);

        assertThatThrownBy(() -> chatRoomService.createOffer(-1L, buyer.getId(), request))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getHttpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createOffer_판매중아닌상품() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SOLD);
        ChatRoomRequest request = new ChatRoomRequest(item.getId(), "가격 제안합니다", 8000);

        assertThatThrownBy(() -> chatRoomService.createOffer(item.getId(), buyer.getId(), request))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createOffer_본인상품() {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoomRequest request = new ChatRoomRequest(item.getId(), "가격 제안합니다", 8000);

        assertThatThrownBy(() -> chatRoomService.createOffer(item.getId(), seller.getId(), request))
                .isInstanceOf(ChatRoomException.class)
                .extracting(e -> ((ChatRoomException) e).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectOffer_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoomRequest request = new ChatRoomRequest(item.getId(), "가격 제안합니다", 8000);
        ChatRoomAndMessageDto offerResult = chatRoomService.createOffer(item.getId(), buyer.getId(), request);

        ChatRoomAndMessageDto result = chatRoomService.rejectOffer(offerResult.room().roomId(), seller.getId(), offerResult.message().messageId());

        assertThat(result.message().offerStatus()).isEqualTo(OfferStatus.REJECTED);
        Orders order = ordersRepository.findById(result.message().orderId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void rejectOffer_판매자아닌_권한없음() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(item, buyer));

        assertThatThrownBy(() -> chatRoomService.rejectOffer(chatRoom.getId(), buyer.getId(), -1L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectOffer_다른방의_제안() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item1 = createAndSaveItem(seller, ItemStatus.SELLING);
        Item item2 = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoomAndMessageDto offerResult = chatRoomService.createOffer(item1.getId(), buyer.getId(),
                new ChatRoomRequest(item1.getId(), "가격 제안합니다", 8000));
        ChatRoom room2 = chatRoomRepository.save(new ChatRoom(item2, buyer));

        assertThatThrownBy(() -> chatRoomService.rejectOffer(room2.getId(), seller.getId(), offerResult.message().messageId()))
                .isInstanceOf(ChatMessageException.class);
    }

    @Test
    void rejectOffer_이미처리된제안() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoomAndMessageDto offerResult = chatRoomService.createOffer(item.getId(), buyer.getId(),
                new ChatRoomRequest(item.getId(), "가격 제안합니다", 8000));
        chatRoomService.rejectOffer(offerResult.room().roomId(), seller.getId(), offerResult.message().messageId());

        assertThatThrownBy(() -> chatRoomService.rejectOffer(offerResult.room().roomId(), seller.getId(), offerResult.message().messageId()))
                .isInstanceOf(ChatMessageException.class);
    }

    @Test
    void acceptOffer_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoomAndMessageDto offerResult = chatRoomService.createOffer(item.getId(), buyer.getId(),
                new ChatRoomRequest(item.getId(), "가격 제안합니다", 8000));

        ChatRoomAndMessageDto result = chatRoomService.acceptOffer(offerResult.room().roomId(), seller.getId(), offerResult.message().messageId());

        assertThat(result.message().offerStatus()).isEqualTo(OfferStatus.ACCEPTED);
        Orders order = ordersRepository.findById(result.message().orderId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(ItemStatus.RESERVED);
    }

    @Test
    void acceptOffer_판매중아닌상품() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ChatRoomAndMessageDto offerResult = chatRoomService.createOffer(item.getId(), buyer.getId(),
                new ChatRoomRequest(item.getId(), "가격 제안합니다", 8000));
        item.changeStatus(ItemStatus.SOLD);
        itemRepository.save(item);

        assertThatThrownBy(() -> chatRoomService.acceptOffer(offerResult.room().roomId(), seller.getId(), offerResult.message().messageId()))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
