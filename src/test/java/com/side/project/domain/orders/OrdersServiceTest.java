package com.side.project.domain.orders;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.orders.ordersdto.OrdersActionResponseDto;
import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import com.side.project.domain.orders.ordersdto.TrackingUpdateDto;
import com.side.project.domain.orders.repository.OrdersRepository;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.member.MemberException;
import com.side.project.config.TestcontainersConfig;
import com.side.project.web.exception.orders.OrdersException;
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
class OrdersServiceTest {

    @Autowired
    private OrdersService ordersService;

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
        Item item = new Item("상품명" + UUID.randomUUID(), "상품설명", 10000, status, Category.TOP, seller);
        item.addItemImage(new ItemImage("original.png", "stored.png"));
        return itemRepository.save(item);
    }

    private Orders createAndSaveOrders(Member buyer, Item item, OrderStatus status, Integer agreedPrice) {
        Orders orders = new Orders();
        orders.createOrders(buyer, item, status, agreedPrice);
        return ordersRepository.save(orders);
    }

    @Test
    void save_즉시구매_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        Orders result = ordersService.save(item.getId(), buyer.getId(), OrderStatus.PAY_COMPLETED);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAY_COMPLETED);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(ItemStatus.RESERVED);
    }

    @Test
    void save_존재하지않는_상품() {
        Member buyer = createAndSaveMember("buyer");

        assertThatThrownBy(() -> ordersService.save(-1L, buyer.getId(), OrderStatus.PAY_COMPLETED))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void save_판매중이_아닌_상품() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SOLD);

        assertThatThrownBy(() -> ordersService.save(item.getId(), buyer.getId(), OrderStatus.PAY_COMPLETED))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void save_본인상품_구매시도() {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        assertThatThrownBy(() -> ordersService.save(item.getId(), seller.getId(), OrderStatus.PAY_COMPLETED))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void save_존재하지않는_회원() {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        assertThatThrownBy(() -> ordersService.save(item.getId(), -1L, OrderStatus.PAY_COMPLETED))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void createNegotiationOrder_신규생성() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        Orders result = ordersService.createNegotiationOrder(item.getId(), buyer.getId(), 8000);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(result.getAgreedPrice()).isEqualTo(8000);
    }

    @Test
    void createNegotiationOrder_기존주문_가격갱신() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        Orders existing = createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 7000);

        Orders result = ordersService.createNegotiationOrder(item.getId(), buyer.getId(), 9000);

        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getAgreedPrice()).isEqualTo(9000);
    }

    @Test
    void rejectNegotiation_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 8000);

        Orders result = ordersService.rejectNegotiation(item.getId(), buyer.getId(), seller.getId());

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void rejectNegotiation_주문없음() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        assertThatThrownBy(() -> ordersService.rejectNegotiation(item.getId(), buyer.getId(), seller.getId()))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectNegotiation_판매자아님() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Member other = createAndSaveMember("other");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 8000);

        assertThatThrownBy(() -> ordersService.rejectNegotiation(item.getId(), buyer.getId(), other.getId()))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void acceptNegotiation_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 8000);

        Orders result = ordersService.acceptNegotiation(item.getId(), buyer.getId(), seller.getId(), 9000);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.RESERVED);
        assertThat(result.getAgreedPrice()).isEqualTo(9000);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(ItemStatus.RESERVED);
    }

    @Test
    void registerTracking_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.RESERVED);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.PAY_COMPLETED, 10000);
        TrackingUpdateDto trackingUpdateDto = new TrackingUpdateDto();
        trackingUpdateDto.setTrackingCompany("CJ대한통운");
        trackingUpdateDto.setTrackingNumber("123456789");

        OrdersResponseDto result = ordersService.registerTracking(orders.getId(), seller.getId(), trackingUpdateDto);

        assertThat(result.getTrackingCompany()).isEqualTo("CJ대한통운");
        assertThat(result.getTrackingNumber()).isEqualTo("123456789");
    }

    @Test
    void registerTracking_주문없음() {
        Member seller = createAndSaveMember("seller");
        TrackingUpdateDto trackingUpdateDto = new TrackingUpdateDto();

        assertThatThrownBy(() -> ordersService.registerTracking(-1L, seller.getId(), trackingUpdateDto))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void registerTracking_판매자아님() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Member other = createAndSaveMember("other");
        Item item = createAndSaveItem(seller, ItemStatus.RESERVED);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.PAY_COMPLETED, 10000);
        TrackingUpdateDto trackingUpdateDto = new TrackingUpdateDto();

        assertThatThrownBy(() -> ordersService.registerTracking(orders.getId(), other.getId(), trackingUpdateDto))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void changeOrderStatus_ACCEPT_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 10000);

        OrdersActionResponseDto result = ordersService.changeOrderStatus(orders.getId(), "ACCEPT", seller.getId());

        assertThat(result.status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(ItemStatus.RESERVED);
    }

    @Test
    void changeOrderStatus_PAY_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.RESERVED);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.RESERVED, 10000);

        OrdersActionResponseDto result = ordersService.changeOrderStatus(orders.getId(), "PAY", buyer.getId());

        assertThat(result.status()).isEqualTo(OrderStatus.PAY_COMPLETED);
    }

    @Test
    void changeOrderStatus_SHIP_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.RESERVED);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.PAY_COMPLETED, 10000);

        OrdersActionResponseDto result = ordersService.changeOrderStatus(orders.getId(), "SHIP", seller.getId());

        assertThat(result.status()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    void changeOrderStatus_CONFIRM_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.RESERVED);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.SHIPPING, 10000);

        OrdersActionResponseDto result = ordersService.changeOrderStatus(orders.getId(), "CONFIRM", buyer.getId());

        assertThat(result.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(ItemStatus.SOLD);
    }

    @Test
    void changeOrderStatus_CANCEL_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.RESERVED);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.RESERVED, 10000);

        OrdersActionResponseDto result = ordersService.changeOrderStatus(orders.getId(), "CANCEL", buyer.getId());

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(ItemStatus.SELLING);
    }

    @Test
    void changeOrderStatus_잘못된순서() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 10000);

        assertThatThrownBy(() -> ordersService.changeOrderStatus(orders.getId(), "PAY", buyer.getId()))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void changeOrderStatus_권한없는_역할() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 10000);

        assertThatThrownBy(() -> ordersService.changeOrderStatus(orders.getId(), "ACCEPT", buyer.getId()))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void changeOrderStatus_알수없는_action() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 10000);

        assertThatThrownBy(() -> ordersService.changeOrderStatus(orders.getId(), "UNKNOWN", seller.getId()))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void changeOrderStatus_완료된주문_재취소불가() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller, ItemStatus.SOLD);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.COMPLETED, 10000);

        assertThatThrownBy(() -> ordersService.changeOrderStatus(orders.getId(), "CANCEL", buyer.getId()))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void changeOrderStatus_권한없는_요청자() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Member other = createAndSaveMember("other");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        Orders orders = createAndSaveOrders(buyer, item, OrderStatus.REQUESTED, 10000);

        assertThatThrownBy(() -> ordersService.changeOrderStatus(orders.getId(), "ACCEPT", other.getId()))
                .isInstanceOf(OrdersException.class)
                .extracting(e -> ((OrdersException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
