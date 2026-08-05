package com.side.project;

import com.side.project.domain.chat.chatroom.ChatRoomService;
import com.side.project.domain.chat.chatroom.dto.ChatRoomAndMessageDto;
import com.side.project.domain.chat.chatroom.dto.ChatRoomRequest;
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
import com.side.project.domain.orders.OrdersService;
import com.side.project.domain.ordershistory.OrdersHistoryService;
import com.side.project.domain.wishlist.WishListService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 개발 환경용 시드 데이터 생성기. 리포지토리에 직접 데이터를 꽂아넣지 않고 실제 서비스 계층
 * (ChatRoomService/OrdersService/WishListService)을 그대로 호출해서, 네고 수락 시 자동 거절되는
 * 경쟁 주문/오퍼나 OrdersHistory 적재 같은 부수효과까지 실제 로직대로 재현한다.
 * spring.jpa.hibernate.ddl-auto=update이므로 데이터는 재부팅해도 유지되며,
 * memberRepository.count() > 0이면 재시딩하지 않는다(최초 1회만 실행).
 */
@Component
@RequiredArgsConstructor
public class InitDb implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final WishListService wishListService;
    private final ChatRoomService chatRoomService;
    private final OrdersService ordersService;
    private final OrdersHistoryService ordersHistoryService;

    private int itemImageSeq = 0;

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0) {
            return;
        }

        List<Member> sellers = createMembers("seller", new String[]{"김판매", "이판매", "박판매", "최판매"});
        List<Member> buyers = createMembers("buyer", new String[]{"정구매", "강구매", "조구매", "윤구매", "장구매", "임구매"});

        List<Item> plainItems = createPlainSellingItems(sellers);

        Item pendingOfferItem = createItem(sellers.get(1), "빈티지 레더 자켓", "가볍게 입기 좋은 빈티지 자켓입니다.", 120000, Category.OUTER);
        Item contestedItem = createItem(sellers.get(2), "한정판 스니커즈", "새 제품, 박스 포함입니다.", 150000, Category.SHOES);
        Item payCompletedItem = createItem(sellers.get(3), "명품 토트백", "정품 구매영수증 있습니다.", 200000, Category.BAG);
        Item completedItemA = createItem(sellers.get(0), "겨울 롱패딩", "한 시즌만 착용했습니다.", 180000, Category.OUTER);
        Item completedItemB = createItem(sellers.get(1), "디자이너 니트", "촉감 좋은 캐시미어 혼방입니다.", 75000, Category.TOP);
        Item canceledItem = createItem(sellers.get(2), "정장 구두", "사이즈가 안 맞아 판매합니다.", 95000, Category.SHOES);

        // 네고 대기중 (아무도 응답하지 않은 오퍼)
        chatRoomService.createOffer(pendingOfferItem.getId(), buyers.get(0).getId(),
                new ChatRoomRequest(pendingOfferItem.getId(), "이 가격에 거래 가능할까요?", 100000));

        // 네고 2명 경합 -> 1건 수락, 나머지 1건은 자동 거절 (채팅방 2명 문의 + 네고 수락/거절 동시 충족)
        ChatRoomAndMessageDto contestedOffer1 = chatRoomService.createOffer(contestedItem.getId(), buyers.get(1).getId(),
                new ChatRoomRequest(contestedItem.getId(), "바로 결제 가능합니다.", 130000));
        chatRoomService.createOffer(contestedItem.getId(), buyers.get(2).getId(),
                new ChatRoomRequest(contestedItem.getId(), "이 가격에 주세요.", 140000));
        chatRoomService.acceptOffer(contestedOffer1.room().roomId(), sellers.get(2).getId(), contestedOffer1.message().messageId());

        // 즉시구매 후 결제완료 상태로 대기 (배송 시작 전)
        Orders payCompletedOrder = ordersService.save(payCompletedItem.getId(), buyers.get(3).getId(), OrderStatus.PAY_COMPLETED);
        ordersHistoryService.save(payCompletedOrder);

        // 거래완료 경로 A: 즉시구매 -> 발송 -> 구매확정
        Orders completedOrderA = ordersService.save(completedItemA.getId(), buyers.get(4).getId(), OrderStatus.PAY_COMPLETED);
        ordersHistoryService.save(completedOrderA);
        ordersService.changeOrderStatus(completedOrderA.getId(), "SHIP", sellers.get(0).getId());
        ordersService.changeOrderStatus(completedOrderA.getId(), "CONFIRM", buyers.get(4).getId());

        // 거래완료 경로 B: 네고 수락 -> 결제 -> 발송 -> 구매확정
        ChatRoomAndMessageDto completedOfferB = chatRoomService.createOffer(completedItemB.getId(), buyers.get(5).getId(),
                new ChatRoomRequest(completedItemB.getId(), "네고 부탁드립니다.", 65000));
        chatRoomService.acceptOffer(completedOfferB.room().roomId(), sellers.get(1).getId(), completedOfferB.message().messageId());
        Long completedOrderBId = completedOfferB.message().orderId();
        ordersService.changeOrderStatus(completedOrderBId, "PAY", buyers.get(5).getId());
        ordersService.changeOrderStatus(completedOrderBId, "SHIP", sellers.get(1).getId());
        ordersService.changeOrderStatus(completedOrderBId, "CONFIRM", buyers.get(5).getId());

        // 취소: 네고 수락 -> 취소 (상품은 다시 판매중으로 복귀)
        ChatRoomAndMessageDto canceledOffer = chatRoomService.createOffer(canceledItem.getId(), buyers.get(0).getId(),
                new ChatRoomRequest(canceledItem.getId(), "네고 제안드립니다.", 85000));
        chatRoomService.acceptOffer(canceledOffer.room().roomId(), sellers.get(2).getId(), canceledOffer.message().messageId());
        ordersService.changeOrderStatus(canceledOffer.message().orderId(), "CANCEL", sellers.get(2).getId());

        // 위시리스트: 같은 상품을 여러 회원이 찜, 한 회원이 여러 상품을 찜
        Item popularItem = plainItems.get(0);
        wishListService.addWishList(popularItem.getId(), buyers.get(0).getId());
        wishListService.addWishList(popularItem.getId(), buyers.get(1).getId());
        wishListService.addWishList(popularItem.getId(), buyers.get(2).getId());
        wishListService.addWishList(plainItems.get(1).getId(), buyers.get(3).getId());
        wishListService.addWishList(plainItems.get(2).getId(), buyers.get(3).getId());
        wishListService.addWishList(plainItems.get(3).getId(), buyers.get(3).getId());
    }

    private List<Member> createMembers(String loginPrefix, String[] names) {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            int seq = i + 1;
            Member member = new Member(
                    loginPrefix + seq,
                    names[i],
                    "password123!",
                    names[i] + "닉네임",
                    new Address(String.format("%05d", seq), "테헤란로 " + seq + "길", "역삼동 " + seq, "상세주소 " + seq)
            );
            members.add(memberRepository.save(member));
        }
        return members;
    }

    private List<Item> createPlainSellingItems(List<Member> sellers) {
        String[][] specs = {
                {"가을 트렌치 코트", "OUTER", "89000"},
                {"베이직 반팔 티셔츠", "TOP", "15000"},
                {"와이드 슬랙스", "BOTTOM", "32000"},
                {"캔버스 백팩", "BAG", "45000"},
                {"가죽 스니커즈", "SHOES", "68000"},
                {"심플 실버 목걸이", "ACCESSORY", "22000"},
                {"후드 집업", "TOP", "39000"},
                {"청 데님 팬츠", "BOTTOM", "41000"},
                {"크로스 숄더백", "BAG", "53000"},
        };

        List<Item> items = new ArrayList<>();
        for (int i = 0; i < specs.length; i++) {
            Member seller = sellers.get(i % sellers.size());
            items.add(createItem(seller, specs[i][0], specs[i][0] + " 판매합니다.",
                    Integer.parseInt(specs[i][2]), Category.valueOf(specs[i][1])));
        }
        return items;
    }

    private Item createItem(Member seller, String name, String description, Integer price, Category category) {
        Item item = new Item(name, description, price, ItemStatus.SELLING, category, seller);
        item.addItemImage(new ItemImage(name + "-1.jpg", "stored-item" + (++itemImageSeq) + "-1.jpg"));
        item.addItemImage(new ItemImage(name + "-2.jpg", "stored-item" + itemImageSeq + "-2.jpg"));
        return itemRepository.save(item);
    }
}
