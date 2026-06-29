package com.side.project.domain.item;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemSaveForm {

    private Long itemId;

    @NotEmpty
    private String name;

    private String description;

    @NotNull
    private Integer price;

}
