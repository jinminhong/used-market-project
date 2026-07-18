package com.side.project.domain.orders.repository;

import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface OrdersRepositoryCustom {
    Slice<OrdersResponseDto> findAllPurchases(Long memberId, Pageable pageable);

    Slice<OrdersResponseDto> findAllSales(Long memberId, Pageable pageable);
}
