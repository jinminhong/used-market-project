package com.side.project.domain.wishlist.wishlistdto;

import com.querydsl.core.annotations.QueryProjection;
import com.side.project.domain.item.ItemStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WishListResponseDto {

    private Long wishListId;
    private Long itemId;
    private String itemName;
    private String description;
    private Integer price;
    private ItemStatus itemStatus;
    private String sellerNickName;
    private String thumbnailFilename;

    @QueryProjection
    public WishListResponseDto(Long wishListId, Long itemId, String itemName, String description, Integer price, ItemStatus itemStatus, String sellerNickName, String thumbnailFilename) {
        this.wishListId = wishListId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.description = description;
        this.price = price;
        this.itemStatus = itemStatus;
        this.sellerNickName = sellerNickName;
        this.thumbnailFilename = thumbnailFilename;
    }
}
