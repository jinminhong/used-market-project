package com.side.project.domain.orders;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.ordershistory.OrdersHistoryRepository;
import com.side.project.domain.orders.repository.OrdersRepository;
import com.side.project.web.exception.item.ItemException;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Transactional을 붙이지 않는다 - 테스트 트랜잭션이 하나의 커넥션에 스레드를 묶으면
 * 실제 동시 접근(별도 커넥션/트랜잭션)이 재현되지 않기 때문이다.
 * 대신 각 테스트가 만든 row를 @AfterEach에서 FK 역순으로 직접 정리한다.
 */
@SpringBootTest
class OrdersServiceConcurrencyTest {

    @Autowired
    private OrdersService ordersService;

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
        // 주의: item/buyer는 Orders/OrdersHistory에 @ManyToOne(LAZY)로 걸려있는 프록시다.
        // Hibernate 프록시는 식별자(getId())만 접근하면 초기화(DB 재조회)를 트리거하지 않지만,
        // 그 프록시를 통해 한 단계 더 들어간 필드(getItem() 등)에 접근하면 세션이 필요해 밖에서는 실패한다.
        // 그래서 item 자체의 id는 바로 비교하되(1단계), history는 orders id로만 매칭한다(1단계).
        List<Long> orderIds = ordersRepository.findAll().stream()
                .filter(o -> o.getItem().getId().equals(item.getId()))
                .map(Orders::getId)
                .toList();
        ordersHistoryRepository.findAll().stream()
                .filter(h -> orderIds.contains(h.getOrders().getId()))
                .forEach(h -> ordersHistoryRepository.deleteById(h.getId()));
        orderIds.forEach(ordersRepository::deleteById);
        itemRepository.deleteById(item.getId());
        memberRepository.deleteById(buyer2.getId());
        memberRepository.deleteById(buyer1.getId());
        memberRepository.deleteById(seller.getId());
    }

    @Test
    void 즉시구매_동시성_한명만_성공한다() throws InterruptedException {
        List<Long> buyerIds = List.of(buyer1.getId(), buyer2.getId());
        int threadCount = buyerIds.size();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        for (Long buyerId : buyerIds) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    ordersService.save(item.getId(), buyerId, OrderStatus.PAY_COMPLETED);
                    successCount.incrementAndGet();
                } catch (ItemException e) {
                    conflictCount.incrementAndGet();
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

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(ItemStatus.RESERVED);
    }

    @Test
    void 네고제안_동시수락_시_레이스컨디션_노출() throws InterruptedException {
        Orders order1 = ordersService.createNegotiationOrder(item.getId(), buyer1.getId(), 8000);
        Orders order2 = ordersService.createNegotiationOrder(item.getId(), buyer2.getId(), 9000);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                ordersService.acceptNegotiation(item.getId(), buyer1.getId(), seller.getId(), 8000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                ordersService.acceptNegotiation(item.getId(), buyer2.getId(), seller.getId(), 9000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long acceptedCount = ordersRepository.findAllById(List.of(order1.getId(), order2.getId())).stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.ACCEPTED)
                .count();

        // acceptNegotiation()은 이제 findByIdWithMemberForUpdate로 상품 행에 PESSIMISTIC_WRITE 락을 건다.
        // 뒤늦게 락을 얻은 스레드는 rejectOtherOrders로 이미 REJECTED 처리된 자신의 주문을
        // REQUESTED 조건으로 다시 조회하다 못 찾아 OrdersException(NOT_FOUND)로 끝난다 (스레드 내부에서 무시됨).
        // 그 결과 정확히 하나만 ACCEPTED로 남는다.
        assertThat(acceptedCount).isEqualTo(1);
    }
}
