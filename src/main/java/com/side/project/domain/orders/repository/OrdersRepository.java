package com.side.project.domain.orders.repository;

import com.side.project.domain.item.Item;
import com.side.project.domain.member.Member;
import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> , OrdersRepositoryCustom {

    Optional<Orders> findByBuyerAndItemAndOrderStatus(Member buyer, Item item, OrderStatus orderStatus);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        UPDATE orders
        SET order_status = 'REJECTED'
        WHERE order_status = 'REQUESTED'
          AND item_id = :itemId
          AND orders_id <> :acceptedOrderId
        """, nativeQuery = true)
    int rejectOtherOrders(@Param("itemId") Long itemId, @Param("acceptedOrderId") Long acceptedOrderId);
}
