package com.side.project.domain.item.embedding;

import com.side.project.config.TestcontainersConfig;
import com.side.project.domain.item.Category;
import com.side.project.domain.item.ItemService;
import com.side.project.domain.item.itemdto.ItemSaveDto;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 저장/수정 시 비동기로 임베딩이 채워지는 전체 파이프라인(ItemService -> 이벤트 -> Gemini 호출 -> DB 반영)을
 * 검증한다. AFTER_COMMIT 이벤트 리스너를 실제로 태우기 위해 다른 서비스 테스트들과 달리 @Transactional을
 * 붙이지 않는다(트랜잭션이 실제로 커밋돼야 이벤트가 발행된다).
 */
@Import(TestcontainersConfig.class)
@SpringBootTest
class ItemEmbeddingPipelineTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long createdItemId;
    private Long createdMemberId;

    @AfterEach
    void cleanUp() {
        if (createdItemId != null) {
            itemRepository.deleteById(createdItemId);
        }
        if (createdMemberId != null) {
            memberRepository.deleteById(createdMemberId);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void save_generatesEmbeddingAsynchronously() throws InterruptedException, IOException {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member seller = memberRepository.save(new Member(
                "embed-test-" + suffix, "홍길동", "password123!", "임베딩테스트" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호")));
        createdMemberId = seller.getId();

        ItemSaveDto saveDto = new ItemSaveDto();
        saveDto.setName("가을 트렌치 코트");
        saveDto.setDescription("쌀쌀한 날씨에 어울리는 겉옷입니다.");
        saveDto.setPrice(89000);
        saveDto.setCategory(Category.OUTER);

        Long itemId = itemService.save(saveDto, seller.getLoginId(), null);
        createdItemId = itemId;

        float[] embedding = waitForEmbedding(itemId);

        assertThat(embedding).isNotNull();
        assertThat(embedding).hasSize(768);
    }

    private float[] waitForEmbedding(Long itemId) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Thread.sleep(500);
            float[] embedding = itemRepository.findById(itemId).orElseThrow().getEmbedding();
            if (embedding != null) {
                return embedding;
            }
        }
        return null;
    }
}
