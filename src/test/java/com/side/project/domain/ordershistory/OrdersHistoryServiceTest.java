package com.side.project.domain.ordershistory;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrdersHistoryServiceTest {

    @Autowired
    private OrdersHistoryService ordersHistoryService;

    @Autowired
    private OrdersHistoryRepository ordersHistoryRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member createAndSaveMember(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member(prefix + suffix, "홍길동", "password123", prefix + "Nick" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호"));
        return memberRepository.save(member);
    }

    @Test
    void save_주문상태_스냅샷_저장() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = new Item("상품명" + UUID.randomUUID(), "설명", 10000, ItemStatus.SELLING, Category.TOP, seller);
        item.addItemImage(new ItemImage("original.png", "stored.png"));
        itemRepository.save(item);

        Orders orders = new Orders();
        orders.createOrders(buyer, item, OrderStatus.ACCEPTED, 10000);
        ordersRepository.save(orders);

        ordersHistoryService.save(orders);

        List<OrdersHistory> histories = ordersHistoryRepository.findAll().stream()
                .filter(h -> h.getOrders().getId().equals(orders.getId()))
                .toList();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }
}
