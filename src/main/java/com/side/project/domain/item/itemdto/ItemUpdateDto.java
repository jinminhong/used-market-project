package com.side.project.domain.item.itemdto;

import com.side.project.domain.item.ItemStatus;
import lombok.Data;

@Data
public class ItemUpdateDto {

    private String name;
    private String description;
    private Integer price;
    private ItemStatus status;
}
