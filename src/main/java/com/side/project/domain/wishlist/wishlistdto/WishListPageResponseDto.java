package com.side.project.domain.wishlist.wishlistdto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WishListPageResponseDto {

    private List<WishListResponseDto> list = new ArrayList<>();

    private boolean hasNext;

    public WishListPageResponseDto(List<WishListResponseDto> list, boolean hasNext) {
        this.list = list;
        this.hasNext = hasNext;
    }
}
