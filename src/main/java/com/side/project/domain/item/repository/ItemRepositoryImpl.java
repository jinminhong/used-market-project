package com.side.project.domain.item.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.side.project.domain.item.Category;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.item.embedding.ItemEmbeddingService;
import com.side.project.domain.item.itemdto.ItemListDto;
import com.side.project.domain.item.itemdto.ItemSearchCondition;
import com.side.project.domain.item.itemdto.QItemListDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.side.project.domain.item.QItem.item;
import static com.side.project.domain.member.QMember.member;
import static org.springframework.util.StringUtils.*;

@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom{

    private static final String VECTOR_SEARCH_SQL = """
            SELECT i.item_id, i.members_id, i.name, i.description, i.price, i.status, i.category,
                   m.nick_name, ii.stored_filename
            FROM items i
            JOIN members m ON m.members_id = i.members_id
            JOIN item_image ii ON ii.id = i.thumbnail_image_id
            WHERE i.embedding IS NOT NULL
              AND (CAST(:category AS varchar) IS NULL OR i.category = CAST(:category AS varchar))
              AND (CAST(:status AS varchar) IS NULL OR i.status = CAST(:status AS varchar))
              AND (CAST(:priceGoe AS integer) IS NULL OR i.price >= CAST(:priceGoe AS integer))
              AND (CAST(:priceLoe AS integer) IS NULL OR i.price <= CAST(:priceLoe AS integer))
            ORDER BY i.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit OFFSET :offset
            """;

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;
    private final ItemEmbeddingService itemEmbeddingService;

    @Override
    public Slice<ItemListDto> searchItems(ItemSearchCondition condition , Pageable pageable) {
        if (hasText(condition.getKeyword())) {
            return searchByVector(condition, pageable);
        }
        return searchByFilters(condition, pageable);
    }

    private Slice<ItemListDto> searchByFilters(ItemSearchCondition condition, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<ItemListDto> itemList = queryFactory.select(new QItemListDto(item.id, item.seller.id, item.name, item.description, item.price,
                        item.status, item.category, item.seller.nickName, item.thumbnailImage.storedFilename))
                .from(item)
                .join(item.seller, member)
                .join(item.thumbnailImage)
                .where(categoryEq(condition.getCategory()),
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

    /**
     * keyword는 이름/설명 의미 기반 벡터 유사도로 대체한다(판매자 닉네임 부분일치는 더 이상 지원하지 않음).
     * QueryDSL은 pgvector의 `<=>` 연산자를 1급으로 지원하지 않아 네이티브 쿼리로 구현한다.
     */
    private Slice<ItemListDto> searchByVector(ItemSearchCondition condition, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int limit = pageSize + 1;

        float[] queryVector = itemEmbeddingService.embed(condition.getKeyword());

        Query query = em.createNativeQuery(VECTOR_SEARCH_SQL)
                .setParameter("category", condition.getCategory() != null ? condition.getCategory().name() : null)
                .setParameter("status", condition.getStatus() != null ? condition.getStatus().name() : null)
                .setParameter("priceGoe", condition.getPriceGoe())
                .setParameter("priceLoe", condition.getPriceLoe())
                .setParameter("queryVector", toVectorLiteral(queryVector))
                .setParameter("limit", limit)
                .setParameter("offset", pageable.getOffset());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<ItemListDto> itemList = rows.stream().map(this::toItemListDto).collect(Collectors.toList());

        boolean hasNext = itemList.size() > pageSize;
        if (hasNext) {
            itemList.remove(pageSize);
        }
        return new SliceImpl<>(itemList, pageable, hasNext);
    }

    private String toVectorLiteral(float[] vector) {
        return IntStream.range(0, vector.length)
                .mapToObj(i -> Float.toString(vector[i]))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private ItemListDto toItemListDto(Object[] row) {
        Long itemId = ((Number) row[0]).longValue();
        Long memberId = ((Number) row[1]).longValue();
        String name = (String) row[2];
        String description = (String) row[3];
        Integer price = ((Number) row[4]).intValue();
        ItemStatus status = ItemStatus.valueOf((String) row[5]);
        Category category = Category.valueOf((String) row[6]);
        String nickName = (String) row[7];
        String thumbnailFilename = (String) row[8];
        return new ItemListDto(itemId, memberId, name, description, price, status, category, nickName, thumbnailFilename);
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
