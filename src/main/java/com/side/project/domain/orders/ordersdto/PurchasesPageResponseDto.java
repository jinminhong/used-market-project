package com.side.project.domain.orders.ordersdto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PurchasesPageResponseDto {

    private List<OrdersResponseDto> list = new ArrayList<>();

    private boolean hasNext;

    public PurchasesPageResponseDto(List<OrdersResponseDto> list, boolean hasNext) {
        this.list = list;
        this.hasNext = hasNext;
    }
}
