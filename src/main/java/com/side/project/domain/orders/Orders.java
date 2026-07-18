package com.side.project.domain.orders;

import com.side.project.domain.BaseEntity;
import com.side.project.domain.item.Item;
import com.side.project.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.*;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Orders extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orders_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false, name = "buyer_id")
    private Member buyer;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus;

    public void createOrders(Member buyer, Item item, OrderStatus orderStatus) {
        this.buyer = buyer;
        this.item = item;
        this.orderStatus = orderStatus;
    }
}
