package com.side.project.domain.item.repository;

import com.side.project.domain.item.itemdto.ItemListDto;
import com.side.project.domain.item.itemdto.ItemSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ItemRepositoryCustom {
    Slice<ItemListDto> searchItems(ItemSearchCondition condition , Pageable pageable);
}
