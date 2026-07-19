package com.side.project.domain.wishlist.repository;

import com.side.project.domain.wishlist.wishlistdto.WishListResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface WishListRepositoryCustom {

    Slice<WishListResponseDto> findAllWishList(Long memberId, Pageable pageable);
}
