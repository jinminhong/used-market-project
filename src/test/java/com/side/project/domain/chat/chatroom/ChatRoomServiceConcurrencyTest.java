package com.side.project.domain.chat.chatroom;

import com.side.project.domain.chat.chatmessage.OfferStatus;
import com.side.project.domain.chat.chatmessage.repository.ChatMessageRepository;
import com.side.project.domain.chat.chatroom.dto.ChatRoomAndMessageDto;
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
import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.Orders;
import com.side.project.domain.orders.repository.OrdersRepository;
import com.side.project.domain.ordershistory.OrdersHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatMessageService.acceptOffer(ChatRoom, Member, ChatMessage, Long)는 엔티티 파라미터를 그대로 받아
 * 호출자의 트랜잭션(영속성 컨텍스트)에 이미 매니지드된 엔티티라고 가정한다. 테스트에서 미리 save()해둔
 * (그래서 detach된) 엔티티를 직접 넘기면 offerChatMessage.changeStatusToAccept()가 dirty checking으로
 * flush되지 않아 실제로는 상태 변경이 저장되지 않는다 — 그래서 반드시 id 기반의
 * ChatRoomService.acceptOffer(roomId, sellerId, messageId)를 통해 각 스레드가 자기 트랜잭션 안에서
 * 새로 조회한 매니지드 엔티티로 흐르게 한다. 이 경로는 OrdersService.acceptNegotiation의
 * rejectOtherOrders와 ChatMessageService.acceptOffer의 rejectOtherOffers를 한 번에 실제로 검증한다.
 * @Transactional은 붙이지 않는다 (진짜 동시 접근 재현을 위해).
 */
@SpringBootTest
class ChatRoomServiceConcurrencyTest {

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

    @Autowired
    private OrdersHistoryRepository ordersHistoryRepository;

    private Member seller;
    private Member buyer1;
    private Member buyer2;
    private Item item;

    private Member createAndSaveMember(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member(prefix + suffix, "이름", "password123", prefix + "Nick" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호"));
        return memberRepository.save(member);
    }

    @BeforeEach
    void setUp() {
        seller = createAndSaveMember("seller");
        buyer1 = createAndSaveMember("buyer1");
        buyer2 = createAndSaveMember("buyer2");
        Item newItem = new Item("동시성테스트상품" + UUID.randomUUID(), "설명", 10000, ItemStatus.SELLING, Category.TOP, seller);
        newItem.addItemImage(new ItemImage("original.png", "stored.png"));
        item = itemRepository.save(newItem);
    }

    @AfterEach
    void tearDown() {
        List<Long> orderIds = ordersRepository.findAll().stream()
                .filter(o -> o.getItem().getId().equals(item.getId()))
                .map(Orders::getId)
                .toList();
        ordersHistoryRepository.findAll().stream()
                .filter(h -> orderIds.contains(h.getOrders().getId()))
                .forEach(h -> ordersHistoryRepository.deleteById(h.getId()));
        orderIds.forEach(ordersRepository::deleteById);

        List<Long> roomIds = chatRoomRepository.findAll().stream()
                .filter(r -> r.getItem().getId().equals(item.getId()))
                .map(ChatRoom::getId)
                .toList();
        chatMessageRepository.findAll().stream()
                .filter(m -> roomIds.contains(m.getChatRoom().getId()))
                .forEach(m -> chatMessageRepository.deleteById(m.getId()));
        roomIds.forEach(chatRoomRepository::deleteById);

        itemRepository.deleteById(item.getId());
        memberRepository.deleteById(buyer2.getId());
        memberRepository.deleteById(buyer1.getId());
        memberRepository.deleteById(seller.getId());
    }

    @Test
    void 오퍼_동시수락_시_레이스컨디션_노출() throws InterruptedException {
        ChatRoomAndMessageDto offer1 = chatRoomService.createOffer(item.getId(), buyer1.getId(),
                new ChatRoomRequest(item.getId(), "제안1", 8000));
        ChatRoomAndMessageDto offer2 = chatRoomService.createOffer(item.getId(), buyer2.getId(),
                new ChatRoomRequest(item.getId(), "제안2", 9000));

        List<Long> roomIds = List.of(offer1.room().roomId(), offer2.room().roomId());
        List<Long> messageIds = List.of(offer1.message().messageId(), offer2.message().messageId());
        List<Long> orderIds = List.of(offer1.message().orderId(), offer2.message().orderId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            Long roomId = roomIds.get(i);
            Long messageId = messageIds.get(i);
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    chatRoomService.acceptOffer(roomId, seller.getId(), messageId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long acceptedOrderCount = ordersRepository.findAllById(orderIds).stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.ACCEPTED)
                .count();
        long acceptedOfferCount = chatMessageRepository.findAllById(messageIds).stream()
                .filter(m -> m.getOfferStatus() == OfferStatus.ACCEPTED)
                .count();

        // ChatRoomService.acceptOffer -> OrdersService.acceptNegotiation은 같은 트랜잭션 안에서
        // findByIdWithMemberForUpdate로 상품 행에 PESSIMISTIC_WRITE 락을 건다. 늦게 락을 얻은 스레드는
        // 이미 rejectOtherOrders로 REJECTED 처리된 자신의 주문을 찾지 못해 예외로 끝나고,
        // 그 뒤에 이어질 chatMessageService.acceptOffer(rejectOtherOffers 포함)까지 도달하지 못한 채
        // 트랜잭션 전체가 롤백된다. 그 결과 주문/오퍼 모두 정확히 하나만 ACCEPTED로 남는다.
        assertThat(acceptedOrderCount).isEqualTo(1);
        assertThat(acceptedOfferCount).isEqualTo(1);
    }
}
