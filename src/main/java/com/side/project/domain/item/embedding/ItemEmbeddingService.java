package com.side.project.domain.item.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Gemini Developer API(embedContent)를 직접 호출해 텍스트 임베딩을 생성한다.
 * output_dimensionality는 문서상 deprecated 표기된 최상위 필드만 실제로 동작한다(2026-09-01 실측 확인,
 * 신규 embedContentConfig.outputDimensionality는 무시되고 기본 3072차원이 그대로 반환됨).
 */
@Slf4j
@Component
public class ItemEmbeddingService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int outputDimensionality;

    public ItemEmbeddingService(
            RestClient.Builder restClientBuilder,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.embedding.model}") String model,
            @Value("${gemini.embedding.output-dimensionality}") int outputDimensionality) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.apiKey = apiKey;
        this.model = model;
        this.outputDimensionality = outputDimensionality;
    }

    public float[] embed(String text) {
        EmbedContentResponse response = restClient.post()
                .uri("/v1beta/models/{model}:embedContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbedContentRequest(new Content(List.of(new Part(text))), outputDimensionality))
                .retrieve()
                .body(EmbedContentResponse.class);

        if (response == null || response.embedding() == null || response.embedding().values() == null) {
            throw new IllegalStateException("Gemini embedContent 응답에 embedding.values가 없습니다.");
        }

        List<Double> values = response.embedding().values();
        float[] embedding = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            embedding[i] = values.get(i).floatValue();
        }
        return embedding;
    }

    private record EmbedContentRequest(Content content, Integer outputDimensionality) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record EmbedContentResponse(ContentEmbedding embedding) {
    }

    private record ContentEmbedding(List<Double> values) {
    }
}
