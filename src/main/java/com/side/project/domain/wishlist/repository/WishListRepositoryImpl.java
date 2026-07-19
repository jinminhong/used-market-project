package com.side.project.domain.wishlist.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.side.project.domain.item.QItem;
import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import com.side.project.domain.orders.ordersdto.QOrdersResponseDto;
import com.side.project.domain.wishlist.QWishList;
import com.side.project.domain.wishlist.wishlistdto.QWishListResponseDto;
import com.side.project.domain.wishlist.wishlistdto.WishListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static com.side.project.domain.item.QItem.item;
import static com.side.project.domain.itemimage.QItemImage.itemImage;
import static com.side.project.domain.member.QMember.member;
import static com.side.project.domain.orders.QOrders.orders;
import static com.side.project.domain.wishlist.QWishList.*;

@RequiredArgsConstructor
public class WishListRepositoryImpl implements WishListRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<WishListResponseDto> findAllWishList(Long memberId, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        List<WishListResponseDto> wishLists = queryFactory
                .select(new QWishListResponseDto(wishList.id, item.id,item.name,item.description,item.price,item.status,item.seller.nickName,item.thumbnailImage.storedFilename))
                .from(wishList)
                .join(wishList.item, item)
                .where(wishList.member.id.eq(memberId))
                .orderBy(wishList.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = wishLists.size() > pageSize;

        if (hasNext) {
            wishLists.remove(pageSize);
        }
        return new SliceImpl<>(wishLists , pageable , hasNext);
    }
}
