package com.side.project.domain.orders.ordersdto;

import com.querydsl.core.annotations.QueryProjection;
import com.side.project.domain.item.ItemStatus;
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
    private String sellerNickName;
    private String thumbnailFilename;

    private LocalDateTime purchaseDate;
    @QueryProjection
    public OrdersResponseDto(Long orderId, Long itemId, String name, String description, Integer price, ItemStatus status, String sellerNickName, String thumbnailFilename, LocalDateTime purchaseDate) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.sellerNickName = sellerNickName;
        this.thumbnailFilename = thumbnailFilename;
        this.purchaseDate = purchaseDate;
    }
}
