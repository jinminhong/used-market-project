package com.side.project.domain.orders;

import com.side.project.domain.item.Item;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import com.side.project.domain.orders.ordersdto.PurchasesPageResponseDto;
import com.side.project.domain.orders.ordersdto.SalesPageResponseDto;
import com.side.project.domain.orders.ordersdto.TrackingUpdateDto;
import com.side.project.domain.orders.repository.OrdersRepository;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.member.MemberException;
import com.side.project.web.exception.orders.OrdersException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.side.project.domain.item.ItemStatus.*;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdersService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrdersRepository ordersRepository;

    @Transactional
    public void save(Long itemId, Long memberId) {
        Item item = itemRepository.findByIdWithMemberForUpdate(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다"));
        if (!item.getStatus().equals(SELLING)) {
            throw new ItemException("구매할 수 없는 상품입니다");
        }
        if (item.getSeller().getId().equals(memberId)) {
            throw new ItemException("본인이 등록한 상품은 구매할 수 없습니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        Orders orders = new Orders();
        orders.createOrders(member , item ,OrderStatus.PAY_COMPLETED, item.getPrice());
        ordersRepository.save(orders);
        item.changeStatus(RESERVED);
    }

    public PurchasesPageResponseDto getPurchasesList(Long memberId , Pageable pageable) {
        Slice<OrdersResponseDto> purchases = ordersRepository.findAllPurchases(memberId ,pageable);
        return new PurchasesPageResponseDto(purchases.getContent(), purchases.hasNext());
    }

    public SalesPageResponseDto getSalesList(Long memberId, OrderStatus status, Pageable pageable) {
        Slice<OrdersResponseDto> sales = ordersRepository.findAllSales(memberId, status, pageable);
        return new SalesPageResponseDto(sales.getContent(), sales.hasNext());
    }

    @Transactional
    public Long createOrders(Member buyer,Item item,OrderStatus orderStatus, Integer agreedPrice) {
        Orders orders = new Orders();
        orders.createOrders(buyer,item,orderStatus,agreedPrice);
        ordersRepository.save(orders);
        return orders.getId();
    }

    @Transactional
    public OrdersResponseDto registerTracking(Long orderId, Long memberId, TrackingUpdateDto trackingUpdateDto) {
        Orders orders = ordersRepository.findById(orderId).orElseThrow(() -> new OrdersException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        Item item = orders.getItem();
        if (!item.getSeller().getId().equals(memberId)) {
            throw new OrdersException(HttpStatus.FORBIDDEN, "본인이 판매한 주문만 운송장을 등록할 수 있습니다.");
        }
        orders.registerTracking(trackingUpdateDto.getTrackingCompany(), trackingUpdateDto.getTrackingNumber());

        return new OrdersResponseDto(
                orders.getId(),
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getStatus(),
                orders.getOrderStatus(),
                item.getSeller().getNickName(),
                item.getThumbnailImage().getStoredFilename(),
                orders.getTrackingCompany(),
                orders.getTrackingNumber(),
                orders.getLastModifiedDate(),
                orders.getAgreedPrice()
        );
    }
}
