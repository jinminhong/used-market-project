package com.side.project.domain.ordershistory;

import com.side.project.domain.orders.Orders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdersHistoryService {

    private final OrdersHistoryRepository ordersHistoryRepository;

    @Transactional
    public void save(Orders orders) {
        OrdersHistory ordersHistory = new OrdersHistory(orders, orders.getOrderStatus());
        ordersHistoryRepository.save(ordersHistory);
    }

}
