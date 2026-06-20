package com.side.project.domain.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public Long save(Item item) {
        if (item.getStatus() == null) {
            item.setStatus(ItemStatus.SELLING);
        }
        return itemRepository.save(item).getId();
    }

    public Optional<Item> findById(Long itemId) {
        return itemRepository.findById(itemId);
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Transactional
    public void update(Long itemId, ItemUpdateParam updateParam) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + itemId));
        item.updateItem(updateParam);
    }

    @Transactional
    public void delete(Long itemId) {
        itemRepository.deleteById(itemId);
    }
}
