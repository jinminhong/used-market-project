package com.side.project.domain.item.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 상품 name+description으로부터 임베딩 캐시 키를 계산한다. 이 해시가 이전과 같으면
 * 재임베딩(Gemini API 호출)을 건너뛴다.
 */
public final class ItemEmbeddingSourceHash {

    private ItemEmbeddingSourceHash() {
    }

    public static String of(String name, String description) {
        String source = (name == null ? "" : name) + "\n" + (description == null ? "" : description);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    public static String embeddingText(String name, String description) {
        if (description == null || description.isBlank()) {
            return name;
        }
        return name + " " + description;
    }
}
