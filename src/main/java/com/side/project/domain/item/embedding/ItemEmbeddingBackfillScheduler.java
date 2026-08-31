package com.side.project.domain.item.embedding;

import com.side.project.domain.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 이벤트 유실(재시작 타이밍 등)로 임베딩이 채워지지 않은 상품을 주기적으로 재발행하는 안전망.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemEmbeddingBackfillScheduler {

    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 */10 * * * *")
    public void republishMissingEmbeddings() {
        List<Long> itemIds = itemRepository.findIdsWithoutEmbedding();
        if (itemIds.isEmpty()) {
            return;
        }
        log.info("임베딩이 없는 상품 {}건을 재발행합니다.", itemIds.size());
        itemIds.forEach(itemId -> eventPublisher.publishEvent(new ItemEmbeddingRequestedEvent(itemId)));
    }
}
