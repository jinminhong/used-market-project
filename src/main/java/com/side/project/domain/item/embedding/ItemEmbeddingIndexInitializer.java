package com.side.project.domain.item.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * items.embedding에 대한 HNSW 인덱스를 기동 시 보장한다. HNSW는 IVFFlat과 달리 학습(clustering) 단계가
 * 없어 데이터가 없거나 InitDb 시딩 전/후 어느 시점에 실행돼도 무방하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemEmbeddingIndexInitializer implements ApplicationRunner {

    private static final String CREATE_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS items_embedding_hnsw_idx " +
            "ON items USING hnsw (embedding vector_cosine_ops)";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_INDEX_SQL);
            log.info("items_embedding_hnsw_idx 인덱스를 확인/생성했습니다.");
        }
    }
}
