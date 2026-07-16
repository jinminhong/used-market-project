package com.side.project.domain.item.itemdto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageResponseDto {

    private List<ItemListDto> list = new ArrayList<>();

    private boolean hasNext;

    public PageResponseDto(List<ItemListDto> list, boolean hasNext) {
        this.list = list;
        this.hasNext = hasNext;
    }
}
