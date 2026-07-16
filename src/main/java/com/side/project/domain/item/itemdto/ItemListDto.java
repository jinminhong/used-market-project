package com.side.project.domain.item.itemdto;

import com.querydsl.core.annotations.QueryProjection;
import com.side.project.domain.item.Category;
import com.side.project.domain.item.ItemStatus;
import lombok.Data;

@Data
public class ItemListDto {

    private Long itemId;
    private Long memberId;
    private String name ;
    private String description;
    private Integer price;
    private ItemStatus status;
    private Category category;
    private String nickName;
    private String thumbnailFilename;

    @QueryProjection
    public ItemListDto(Long itemId, Long memberId, String name, String description, Integer price, ItemStatus status, Category category, String nickName, String thumbnailFilename) {
        this.itemId = itemId;
        this.memberId = memberId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.category = category;
        this.nickName = nickName;
        this.thumbnailFilename = thumbnailFilename;
    }


}
