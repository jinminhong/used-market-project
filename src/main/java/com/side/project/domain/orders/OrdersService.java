package com.side.project.domain.orders;

import com.side.project.domain.activitylog.ActivityType;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.orders.ordersdto.OrdersActionResponseDto;
import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import com.side.project.domain.orders.ordersdto.PurchasesPageResponseDto;
import com.side.project.domain.orders.ordersdto.SalesPageResponseDto;
import com.side.project.domain.orders.ordersdto.TrackingUpdateDto;
import com.side.project.domain.orders.repository.OrdersRepository;
import com.side.project.domain.ordershistory.OrdersHistoryService;
import com.side.project.web.exception.ErrorCode;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.member.MemberException;
import com.side.project.web.exception.orders.OrdersException;
import com.side.project.web.aop.LogUserActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.side.project.domain.item.ItemStatus.*;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdersService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrdersRepository ordersRepository;
    private final OrdersHistoryService ordersHistoryService;

    @Transactional
    @LogUserActivity(type = ActivityType.ORDER_CREATE, itemIdExpr = "#itemId", memberIdExpr = "#buyerId")
    public Orders save(Long itemId, Long buyerId ,OrderStatus orderStatus) {
        Item item = itemRepository.findByIdWithMemberForUpdate(itemId).orElseThrow(() -> new ItemException(ErrorCode.NOT_FOUND_ITEM, "상품을 찾을 수 없습니다"));
        if (!item.getStatus().equals(SELLING)) {
            throw new ItemException(ErrorCode.CONFLICT_STATE, "구매할 수 없는 상품입니다");
        }
        if (item.getSeller().getId().equals(buyerId)) {
            throw new ItemException(ErrorCode.FORBIDDEN, "본인이 등록한 상품은 구매할 수 없습니다.");
        }
        Member member = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        Orders orders = new Orders();
        orders.createOrders(member , item ,orderStatus, item.getPrice());
        ordersRepository.save(orders);
        item.changeStatus(RESERVED);
        return orders;
    }

    @Transactional
    public Orders createNegotiationOrder(Long itemId, Long buyerId, Integer offeredPrice) {
        Item item = itemRepository.findByIdWithMember(itemId).orElseThrow(() -> new ItemException(ErrorCode.NOT_FOUND_ITEM, "상품을 찾을 수 없습니다"));
        if (!item.getStatus().equals(SELLING)) {
            throw new ItemException(ErrorCode.CONFLICT_STATE, "구매할 수 없는 상품입니다");
        }
        if (item.getSeller().getId().equals(buyerId)) {
            throw new ItemException(ErrorCode.FORBIDDEN, "본인이 등록한 상품은 구매할 수 없습니다.");
        }
        Member buyer = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        return ordersRepository.findByBuyerAndItemAndOrderStatus(buyer, item, OrderStatus.REQUESTED)
                .map(existing -> {
                    existing.updateAgreedPrice(offeredPrice);
                    return existing;
                })
                .orElseGet(() -> {
                    Orders orders = new Orders();
                    orders.createOrders(buyer, item, OrderStatus.REQUESTED, offeredPrice);
                    return ordersRepository.save(orders);
                });
    }

    @Transactional
    public Orders rejectNegotiation(Long itemId, Long buyerId, Long sellerId) {
        Item item = itemRepository.findByIdWithMember(itemId).orElseThrow(() -> new ItemException(ErrorCode.NOT_FOUND_ITEM, "상품을 찾을 수 없습니다"));
        Member buyer = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        Orders orders = ordersRepository.findByBuyerAndItemAndOrderStatus(buyer, item, OrderStatus.REQUESTED)
                .orElseThrow(() -> new OrdersException(ErrorCode.NOT_FOUND_ORDER, "주문을 찾을 수 없습니다."));

        orders.reject(sellerId);
        ordersHistoryService.save(orders);
        return orders;
    }

    @Transactional
    public Orders acceptNegotiation(Long itemId, Long buyerId, Long sellerId, Integer offeredPrice) {
        Item item = itemRepository.findByIdWithMemberForUpdate(itemId).orElseThrow(() -> new ItemException(ErrorCode.NOT_FOUND_ITEM, "상품을 찾을 수 없습니다"));
        Member buyer = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        Orders orders = ordersRepository.findByBuyerAndItemAndOrderStatus(buyer, item, OrderStatus.REQUESTED)
                .orElseThrow(() -> new OrdersException(ErrorCode.NOT_FOUND_ORDER, "주문을 찾을 수 없습니다."));

        orders.updateAgreedPrice(offeredPrice);
        orders.accept(sellerId);
        ordersRepository.rejectOtherOrders(item.getId(), orders.getId());
        ordersHistoryService.save(orders);
        return orders;
    }

    public PurchasesPageResponseDto getPurchasesList(Long memberId , List<OrderStatus> statuses, Pageable pageable) {
        Slice<OrdersResponseDto> purchases = ordersRepository.findAllPurchases(memberId , statuses, pageable);
        return new PurchasesPageResponseDto(purchases.getContent(), purchases.hasNext());
    }

    public SalesPageResponseDto getSalesList(Long memberId, List<OrderStatus> statuses, Pageable pageable) {
        Slice<OrdersResponseDto> sales = ordersRepository.findItemsWithOrderStatus(memberId, statuses, pageable);
        return new SalesPageResponseDto(sales.getContent(), sales.hasNext());
    }

    @Transactional
    public OrdersResponseDto registerTracking(Long orderId, Long memberId, TrackingUpdateDto trackingUpdateDto) {
        Orders orders = ordersRepository.findById(orderId).orElseThrow(() -> new OrdersException(ErrorCode.NOT_FOUND_ORDER, "주문을 찾을 수 없습니다."));
        Item item = orders.getItem();
        if (!item.getSeller().getId().equals(memberId)) {
            throw new OrdersException(ErrorCode.FORBIDDEN, "본인이 판매한 주문만 운송장을 등록할 수 있습니다.");
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

    @Transactional
    @LogUserActivity(typeExpr = "#action", itemIdExpr = "#result.itemId()", memberIdExpr = "#requesterId")
    public OrdersActionResponseDto changeOrderStatus(Long orderId, String action, Long requesterId) {
        Orders orders = ordersRepository.findById(orderId)
                .orElseThrow(() -> new OrdersException(ErrorCode.NOT_FOUND_ORDER, "주문을 찾을 수 없습니다."));
        Item item = orders.getItem();
        boolean isBuyer = orders.getBuyer().getId().equals(requesterId);
        boolean isSeller = item.getSeller().getId().equals(requesterId);
        if (!isBuyer && !isSeller) {
            throw new OrdersException(ErrorCode.FORBIDDEN, "이 주문을 처리할 권한이 없습니다.");
        }

        switch (action) {
            case "ACCEPT" -> {
                orders.accept(requesterId);
                ordersRepository.rejectOtherOrders(item.getId(), orders.getId());
            }
            case "PAY" -> orders.pay(requesterId);
            case "SHIP" -> orders.ship(requesterId);
            case "CONFIRM" -> orders.confirm(requesterId);
            case "CANCEL" -> orders.cancel(requesterId);
            default -> throw new OrdersException(ErrorCode.CONFLICT_STATE, "알 수 없는 action 입니다.");
        }

        ordersHistoryService.save(orders);
        return new OrdersActionResponseDto(orders.getId(), item.getId(), orders.getOrderStatus(), item.getStatus());
    }
}
