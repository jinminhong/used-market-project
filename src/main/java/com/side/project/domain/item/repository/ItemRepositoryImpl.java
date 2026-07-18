package com.side.project.domain.item.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.side.project.domain.item.Category;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.item.itemdto.ItemListDto;
import com.side.project.domain.item.itemdto.ItemSearchCondition;
import com.side.project.domain.item.itemdto.QItemListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static com.side.project.domain.item.QItem.item;
import static com.side.project.domain.member.QMember.member;
import static org.springframework.util.StringUtils.*;

@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ItemListDto> searchItems(ItemSearchCondition condition , Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<ItemListDto> itemList = queryFactory.select(new QItemListDto(item.id, item.member.id, item.name, item.description, item.price,
                        item.status, item.category, item.member.nickName, item.thumbnailImage.storedFilename))
                .from(item)
                .join(item.member, member)
                .join(item.thumbnailImage)
                .where(keywordLike(condition.getKeyword()),
                        categoryEq(condition.getCategory()),
                        statusEq(condition.getStatus()),
                        priceGoe(condition.getPriceGoe()),
                        priceLoe(condition.getPriceLoe()))
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = itemList.size() > pageSize;

        if (hasNext) {
            itemList.remove(pageSize);
        }
        return new SliceImpl<>(itemList, pageable, hasNext);
    }

    private BooleanExpression keywordLike(String keyword) {
        return hasText(keyword) ? item.name.like("%" + keyword + "%").or(member.nickName.like("%" + keyword + "%")) : null;
    }

    private BooleanExpression categoryEq(Category category) {
        return category != null ? item.category.eq(category) : null;
    }

    private BooleanExpression statusEq(ItemStatus status) {
        return status != null ? item.status.eq(status) : null;
    }

    private BooleanExpression priceGoe(Integer priceGoe) {
        return priceGoe != null ? item.price.goe(priceGoe) : null;
    }

    private BooleanExpression priceLoe(Integer priceLoe) {
        return priceLoe != null ? item.price.loe(priceLoe) : null;
    }
}
