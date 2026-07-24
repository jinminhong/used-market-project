package com.side.project.domain.orders.ordersdto;

import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.orders.OrderStatus;

public record OrdersActionResponseDto(Long orderId, Long itemId, OrderStatus status, ItemStatus itemStatus) {
}
