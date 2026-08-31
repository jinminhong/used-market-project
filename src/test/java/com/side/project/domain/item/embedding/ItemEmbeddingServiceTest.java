package com.side.project.domain.item.embedding;

import com.side.project.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfig.class)
@SpringBootTest
class ItemEmbeddingServiceTest {

    @Autowired
    private ItemEmbeddingService itemEmbeddingService;

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void embed_returns768DimensionVector() {
        float[] embedding = itemEmbeddingService.embed("가을 트렌치 코트");

        assertThat(embedding).hasSize(768);
    }
}
