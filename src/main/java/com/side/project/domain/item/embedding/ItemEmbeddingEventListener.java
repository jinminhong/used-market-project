package com.side.project.domain.item.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemEmbeddingEventListener {

    private final ItemEmbeddingWriter itemEmbeddingWriter;

    @Async("itemEmbeddingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(ItemEmbeddingRequestedEvent event) {
        try {
            itemEmbeddingWriter.generateAndSave(event.itemId());
        } catch (Exception e) {
            log.warn("상품 임베딩 생성에 실패했습니다. itemId={}", event.itemId(), e);
        }
    }
}
