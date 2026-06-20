package com.side.project.domain.item;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemSaveForm {

    @NotEmpty
    private String title;

    private String description;

    @NotNull
    private Integer price;

    @NotNull
    private Long sellerId;
}
