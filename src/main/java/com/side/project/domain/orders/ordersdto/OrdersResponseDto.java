package com.side.project.domain.orders.ordersdto;

import com.querydsl.core.annotations.QueryProjection;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.orders.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrdersResponseDto {

    private Long orderId;
    private Long itemId;
    private String name;
    private String description;
    private Integer price;
    private ItemStatus status;
    private OrderStatus orderStatus;
    private String sellerNickName;
    private String thumbnailFilename;
    private String trackingCompany;
    private String trackingNumber;

    private LocalDateTime purchaseDate;
    private Integer agreedPrice;

    @QueryProjection
    public OrdersResponseDto(Long orderId, Long itemId, String name, String description, Integer price, ItemStatus status, OrderStatus orderStatus, String sellerNickName, String thumbnailFilename, String trackingCompany, String trackingNumber, LocalDateTime purchaseDate, Integer agreedPrice) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.orderStatus = orderStatus;
        this.sellerNickName = sellerNickName;
        this.thumbnailFilename = thumbnailFilename;
        this.trackingCompany = trackingCompany;
        this.trackingNumber = trackingNumber;
        this.purchaseDate = purchaseDate;
        this.agreedPrice = agreedPrice;
    }
}
