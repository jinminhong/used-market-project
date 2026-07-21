package com.side.project.domain.wishlist.repository;

import com.side.project.domain.wishlist.WishList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList,Long> , WishListRepositoryCustom {

    Boolean existsByItemIdAndMemberId(Long itemId, Long memberId);

    Optional<WishList> findWishListByItemIdAndMemberId(Long itemId, Long memberId);
}
