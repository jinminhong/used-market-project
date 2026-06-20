package com.side.project.domain.item;

import lombok.Data;

@Data
public class ItemUpdateParam {

    private String title;
    private String description;
    private Integer price;
    private ItemStatus status;
}
