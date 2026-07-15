package com.side.project.domain.orders;

import com.side.project.domain.item.ItemRepository;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdersService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrdersRepository ordersRepository;

    @Transactional
    public OrdersResponseDto orders() {

    }

}
