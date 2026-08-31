package com.side.project.domain.item.embedding;

import com.side.project.domain.item.Item;
import com.side.project.domain.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ItemEmbeddingEventListener.handle()의 self-invocation(같은 빈 내부 호출)은 @Transactional
 * 프록시를 우회하므로, 트랜잭션이 필요한 실제 저장 로직을 별도 빈으로 분리했다.
 */
@Component
@RequiredArgsConstructor
public class ItemEmbeddingWriter {

    private final ItemRepository itemRepository;
    private final ItemEmbeddingService itemEmbeddingService;

    @Transactional
    public void generateAndSave(Long itemId) {
        Item item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            return;
        }

        String hash = ItemEmbeddingSourceHash.of(item.getName(), item.getDescription());
        if (hash.equals(item.getEmbeddingSourceHash()) && item.getEmbedding() != null) {
            return;
        }

        String text = ItemEmbeddingSourceHash.embeddingText(item.getName(), item.getDescription());
        float[] embedding = itemEmbeddingService.embed(text);
        itemRepository.updateEmbedding(itemId, embedding, hash);
    }
}
