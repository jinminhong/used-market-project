package com.side.project.domain.orders;

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
    public Orders save(Long itemId, Long buyerId ,OrderStatus orderStatus) {
        Item item = itemRepository.findByIdWithMemberForUpdate(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다"));
        if (!item.getStatus().equals(SELLING)) {
            throw new ItemException("구매할 수 없는 상품입니다");
        }
        if (item.getSeller().getId().equals(buyerId)) {
            throw new ItemException("본인이 등록한 상품은 구매할 수 없습니다.");
        }
        Member member = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        Orders orders = new Orders();
        orders.createOrders(member , item ,orderStatus, item.getPrice());
        ordersRepository.save(orders);
        item.changeStatus(RESERVED);
        return orders;
    }

    /**
     * 채팅 가격 제안 시 REQUESTED 주문을 생성한다. 같은 (buyer, item) 조합에 이미 REQUESTED 주문이 있으면
     * 새로 만들지 않고 제안가만 갱신한다. 한 아이템에 여러 구매자가 동시에 제안할 수 있어야 하므로
     * item 행을 잠그거나 item 상태를 바꾸지 않는다.
     */
    @Transactional
    public Orders createNegotiationOrder(Long itemId, Long buyerId, Integer offeredPrice) {
        Item item = itemRepository.findByIdWithMember(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다"));
        if (!item.getStatus().equals(SELLING)) {
            throw new ItemException("구매할 수 없는 상품입니다");
        }
        if (item.getSeller().getId().equals(buyerId)) {
            throw new ItemException("본인이 등록한 상품은 구매할 수 없습니다.");
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

    /**
     * 채팅에서 판매자가 제안을 수락할 때, createNegotiationOrder가 만든 REQUESTED 주문을 찾아 ACCEPTED로 전이시킨다.
     * 같은 아이템의 다른 구매자 REQUESTED 주문들은 REJECTED로 일괄 처리된다.
     */
    @Transactional
    public Orders acceptNegotiation(Long itemId, Long buyerId, Long sellerId, Integer offeredPrice) {
        Item item = itemRepository.findByIdWithMember(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다"));
        Member buyer = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        Orders orders = ordersRepository.findByBuyerAndItemAndOrderStatus(buyer, item, OrderStatus.REQUESTED)
                .orElseThrow(() -> new OrdersException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));

        requireRole(item.getSeller().getId().equals(sellerId), "판매자만 승인할 수 있습니다.");

        orders.updateAgreedPrice(offeredPrice);
        applyAccept(orders, item);
        ordersHistoryService.save(orders);
        return orders;
    }

    private void applyAccept(Orders orders, Item item) {
        orders.updateOrderStatus(OrderStatus.ACCEPTED);
        item.changeStatus(RESERVED);
        ordersRepository.rejectOtherOrders(item.getId(), orders.getId());
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

    @Transactional
    public OrdersActionResponseDto changeOrderStatus(Long orderId, String action, Long requesterId) {
        Orders orders = ordersRepository.findById(orderId)
                .orElseThrow(() -> new OrdersException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        Item item = orders.getItem();
        boolean isBuyer = orders.getBuyer().getId().equals(requesterId);
        boolean isSeller = item.getSeller().getId().equals(requesterId);
        if (!isBuyer && !isSeller) {
            throw new OrdersException(HttpStatus.FORBIDDEN, "이 주문을 처리할 권한이 없습니다.");
        }

        OrderStatus before = orders.getOrderStatus();
        switch (action) {
            case "ACCEPT" -> {
                requireStatus(before, OrderStatus.REQUESTED);
                requireRole(isSeller, "판매자만 승인할 수 있습니다.");
                applyAccept(orders, item);
            }
            case "PAY" -> {
                requireStatus(before, OrderStatus.ACCEPTED);
                requireRole(isBuyer, "구매자만 결제할 수 있습니다.");
                orders.updateOrderStatus(OrderStatus.PAY_COMPLETED);
            }
            case "SHIP" -> {
                requireStatus(before, OrderStatus.PAY_COMPLETED);
                requireRole(isSeller, "판매자만 발송 처리할 수 있습니다.");
                orders.updateOrderStatus(OrderStatus.SHIPPING);
            }
            case "CONFIRM" -> {
                requireStatus(before, OrderStatus.SHIPPING);
                requireRole(isBuyer, "구매자만 구매확정할 수 있습니다.");
                orders.updateOrderStatus(OrderStatus.COMPLETED);
                item.changeStatus(SOLD);
            }
            case "CANCEL" -> {
                if (before == OrderStatus.COMPLETED || before == OrderStatus.CANCELED) {
                    throw new OrdersException(HttpStatus.CONFLICT, "취소할 수 없는 주문입니다.");
                }
                if (before == OrderStatus.PAY_COMPLETED || before == OrderStatus.SHIPPING) {
                    requireRole(isSeller, "판매자 동의가 필요합니다.");
                }
                orders.updateOrderStatus(OrderStatus.CANCELED);
                item.changeStatus(SELLING);
            }
            default -> throw new OrdersException(HttpStatus.CONFLICT, "알 수 없는 action 입니다.");
        }

        ordersHistoryService.save(orders);
        return new OrdersActionResponseDto(orders.getId(), item.getId(), orders.getOrderStatus(), item.getStatus());
    }

    private void requireStatus(OrderStatus before, OrderStatus expected) {
        if (before != expected) {
            throw new OrdersException(HttpStatus.CONFLICT, "잘못된 순서의 상태 변경입니다.");
        }
    }

    private void requireRole(boolean allowed, String message) {
        if (!allowed) {
            throw new OrdersException(HttpStatus.FORBIDDEN, message);
        }
    }
}
