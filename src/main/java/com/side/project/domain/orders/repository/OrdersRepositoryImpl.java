package com.side.project.domain.orders.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import com.side.project.domain.orders.ordersdto.QOrdersPurchasesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static com.side.project.domain.item.QItem.*;
import static com.side.project.domain.itemimage.QItemImage.*;
import static com.side.project.domain.member.QMember.*;
import static com.side.project.domain.orders.QOrders.*;

@RequiredArgsConstructor
public class OrdersRepositoryImpl implements OrdersRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<OrdersResponseDto> findAllPurchases(Long memberId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<OrdersResponseDto> purchasesList = queryFactory.select(new QOrdersPurchasesDto(orders.id, orders.item.id, orders.item.name, orders.item.description, orders.item.price, orders.item.status, orders.item.member.nickName, orders.item.thumbnailImage.storedFilename, orders.lastModifiedDate))
                .from(orders)
                .join(orders.item, item)
                .join(orders.item.thumbnailImage, itemImage)
                .join(orders.item.member, member)
                .where(orders.buyer.id.eq(memberId), orders.orderStatus.in(OrderStatus.COMPLETED, OrderStatus.PAY_COMPLETED))
                .orderBy(orders.lastModifiedDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = purchasesList.size() > pageSize;

        if (hasNext) {
            purchasesList.remove(pageSize);
        }
        return new SliceImpl<>(purchasesList , pageable , hasNext);
    }

    @Override
    public Slice<OrdersResponseDto> findAllSales(Long memberId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<OrdersResponseDto> salesList = queryFactory.select(new QOrdersPurchasesDto(orders.id, orders.item.id, orders.item.name, orders.item.description, orders.item.price, orders.item.status, orders.item.member.nickName, orders.item.thumbnailImage.storedFilename, orders.lastModifiedDate))
                .from(orders)
                .join(orders.item, item)
                .join(orders.item.thumbnailImage, itemImage)
                .join(orders.item.member, member)
                .where(item.member.id.eq(memberId), orders.orderStatus.in(OrderStatus.COMPLETED, OrderStatus.PAY_COMPLETED))
                .orderBy(orders.lastModifiedDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = salesList.size() > pageSize;

        if (hasNext) {
            salesList.remove(pageSize);
        }
        return new SliceImpl<>(salesList , pageable , hasNext);
    }
}
