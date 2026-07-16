package com.side.project.domain.item.itemdto;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.ItemStatus;
import lombok.Data;

@Data
public class ItemSearchCondition {

    private Integer priceGoe;
    private Integer priceLoe;

    private Category category;

    private ItemStatus status;

    private String keyword;
}
