package com.side.project.domain.wishlist.repository;

import com.side.project.domain.wishlist.WishList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishListRepository extends JpaRepository<WishList,Long> , WishListRepositoryCustom {

    Boolean existsByItemIdAndMemberId(Long itemId, Long memberId);
}
