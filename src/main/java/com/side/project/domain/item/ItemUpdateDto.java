package com.side.project.domain.item;

import lombok.Data;

@Data
public class ItemUpdateDto {

    private String name;
    private String description;
    private Integer price;
    private ItemStatus status;
}
