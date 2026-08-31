package com.side.project.domain.item.embedding;

import com.side.project.config.TestcontainersConfig;
import com.side.project.domain.item.Category;
import com.side.project.domain.item.ItemService;
import com.side.project.domain.item.itemdto.ItemListDto;
import com.side.project.domain.item.itemdto.ItemSaveDto;
import com.side.project.domain.item.itemdto.ItemSearchCondition;
import com.side.project.domain.item.itemdto.PageResponseDto;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * keyword 검색이 LIKE가 아니라 Gemini 임베딩 기반 벡터 유사도로 동작하는지 검증한다.
 * LIKE였다면 절대 매칭되지 않았을 자연어 질의로 확인한다. searchByVector의 JOIN item_image는
 * INNER JOIN이므로(썸네일은 상품 등록 시 항상 필수) 썸네일 없는 상품은 테스트 대상에서 제외된다 -
 * 따라서 실제 등록 흐름과 동일하게 파일을 하나 첨부해서 저장한다.
 */
@Import(TestcontainersConfig.class)
@SpringBootTest
class ItemHybridSearchTest {

    @TempDir
    static Path tempUploadDir;

    @DynamicPropertySource
    static void overrideFileDir(DynamicPropertyRegistry registry) {
        registry.add("file.dir", () -> tempUploadDir.toString() + "/");
    }

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
    void semanticKeyword_findsItemWithoutLexicalOverlap() throws InterruptedException, IOException {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member seller = memberRepository.save(new Member(
                "hybrid-search-" + suffix, "홍길동", "password123!", "하이브리드검색" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호")));
        createdMemberId = seller.getId();

        ItemSaveDto saveDto = new ItemSaveDto();
        saveDto.setName("가을 트렌치 코트");
        saveDto.setDescription("쌀쌀한 날씨에 어울리는 겉옷입니다.");
        saveDto.setPrice(89000);
        saveDto.setCategory(Category.OUTER);

        MultipartFile multipartFile = new MockMultipartFile("file", "original.png", "image/png",
                "dummy-image-content".getBytes(StandardCharsets.UTF_8));

        Long itemId = itemService.save(saveDto, seller.getLoginId(), List.of(multipartFile));
        createdItemId = itemId;

        waitForEmbedding(itemId);

        ItemSearchCondition condition = new ItemSearchCondition();
        condition.setKeyword("쌀쌀할 때 입는 겉옷");

        PageResponseDto result = itemService.searchItems(condition, 0, 10);

        assertThat(result.getList())
                .extracting(ItemListDto::getItemId)
                .contains(itemId);
    }

    private void waitForEmbedding(Long itemId) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Thread.sleep(500);
            if (itemRepository.findById(itemId).orElseThrow().getEmbedding() != null) {
                return;
            }
        }
    }
}
