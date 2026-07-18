package com.side.project.domain.orders;

import com.side.project.domain.item.Item;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import com.side.project.domain.orders.ordersdto.PurchasesPageResponseDto;
import com.side.project.domain.orders.ordersdto.SalesPageResponseDto;
import com.side.project.domain.orders.repository.OrdersRepository;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.member.MemberException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
        if (item.getMember().getId().equals(memberId)) {
            throw new ItemException("본인이 등록한 상품은 구매할 수 없습니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        Orders orders = new Orders();
        orders.createOrders(member , item ,OrderStatus.PAY_COMPLETED);
        ordersRepository.save(orders);
        item.changeStatus(RESERVED);
    }

    public PurchasesPageResponseDto getPurchasesList(Long memberId , Pageable pageable) {
        Slice<OrdersResponseDto> purchases = ordersRepository.findAllPurchases(memberId ,pageable);
        return new PurchasesPageResponseDto(purchases.getContent(), purchases.hasNext());
    }

    public SalesPageResponseDto getSalesList(Long memberId, Pageable pageable) {
        Slice<OrdersResponseDto> sales = ordersRepository.findAllSales(memberId, pageable);
        return new SalesPageResponseDto(sales.getContent(), sales.hasNext());
    }
}
