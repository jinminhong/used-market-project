package com.side.project.domain.orders;

import com.side.project.domain.BaseEntity;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.member.Member;
import com.side.project.web.exception.ErrorCode;
import com.side.project.web.exception.orders.OrdersException;
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

    @Column(length = 50)
    private String trackingCompany;

    @Column(length = 50)
    private String trackingNumber;

    @Column(name = "agreed_price")
    private Integer agreedPrice;

    public void createOrders(Member buyer, Item item, OrderStatus orderStatus, Integer agreedPrice) {
        this.buyer = buyer;
        this.item = item;
        this.orderStatus = orderStatus;
        this.agreedPrice = agreedPrice;
    }

    public void registerTracking(String trackingCompany, String trackingNumber) {
        this.trackingCompany = trackingCompany;
        this.trackingNumber = trackingNumber;
    }

    public void updateAgreedPrice(Integer offeredPrice) {
        this.agreedPrice = offeredPrice;
    }

    public void cancelExpireReservation() {
        if (orderStatus != OrderStatus.RESERVED) {
            throw new OrdersException(ErrorCode.FORBIDDEN,"예약 상태의 주문만 자동 취소");
        }
        this.orderStatus = OrderStatus.CANCELED;
        this.item.reopenSelling();
    }

    public void accept(Long requesterId) {
        requireStatus(OrderStatus.REQUESTED);
        requireSeller(requesterId, "판매자만 승인할 수 있습니다.");
        this.orderStatus = OrderStatus.RESERVED;
        this.item.changeStatus(ItemStatus.RESERVED);
    }

    public void reject(Long requesterId) {
        requireSeller(requesterId, "판매자만 거절할 수 있습니다.");
        this.orderStatus = OrderStatus.REJECTED;
    }

    public void pay(Long requesterId) {
        requireStatus(OrderStatus.RESERVED);
        requireBuyer(requesterId, "구매자만 결제할 수 있습니다.");
        this.orderStatus = OrderStatus.PAY_COMPLETED;
    }

    public void ship(Long requesterId) {
        requireStatus(OrderStatus.PAY_COMPLETED);
        requireSeller(requesterId, "판매자만 발송 처리할 수 있습니다.");
        this.orderStatus = OrderStatus.SHIPPING;
    }

    public void confirm(Long requesterId) {
        requireStatus(OrderStatus.SHIPPING);
        requireBuyer(requesterId, "구매자만 구매확정할 수 있습니다.");
        this.orderStatus = OrderStatus.COMPLETED;
        this.item.changeStatus(ItemStatus.SOLD);
    }

    public void cancel(Long requesterId) {
        if (orderStatus == OrderStatus.COMPLETED || orderStatus == OrderStatus.CANCELED) {
            throw new OrdersException(ErrorCode.CONFLICT_STATE, "취소할 수 없는 주문입니다.");
        }
        if (orderStatus == OrderStatus.PAY_COMPLETED || orderStatus == OrderStatus.SHIPPING) {
            requireSeller(requesterId, "판매자 동의가 필요합니다.");
        }
        this.orderStatus = OrderStatus.CANCELED;
        this.item.changeStatus(ItemStatus.SELLING);
    }

    private void requireStatus(OrderStatus expected) {
        if (this.orderStatus != expected) {
            throw new OrdersException(ErrorCode.CONFLICT_STATE, "잘못된 순서의 상태 변경입니다.");
        }
    }

    private void requireSeller(Long requesterId, String message) {
        if (!this.item.getSeller().getId().equals(requesterId)) {
            throw new OrdersException(ErrorCode.FORBIDDEN, message);
        }
    }

    private void requireBuyer(Long requesterId, String message) {
        if (!this.buyer.getId().equals(requesterId)) {
            throw new OrdersException(ErrorCode.FORBIDDEN, message);
        }
    }
}
