package com.side.project.domain.ordershistory;

import com.side.project.domain.BaseEntity;
import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.Orders;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.*;
import static jakarta.persistence.GenerationType.*;

@Entity
@Getter
@NoArgsConstructor
public class OrdersHistory extends BaseEntity {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "orders_id")
    private Orders orders;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public OrdersHistory(Orders orders, OrderStatus status) {
        this.orders = orders;
        this.status = status;
    }
}
