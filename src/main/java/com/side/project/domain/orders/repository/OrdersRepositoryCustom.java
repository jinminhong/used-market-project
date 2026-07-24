package com.side.project.domain.orders.repository;

import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface OrdersRepositoryCustom {
    Slice<OrdersResponseDto> findAllPurchases(Long memberId, List<OrderStatus> statuses, Pageable pageable);

    Slice<OrdersResponseDto> findItemsWithOrderStatus(Long memberId, List<OrderStatus> statuses, Pageable pageable);
}
