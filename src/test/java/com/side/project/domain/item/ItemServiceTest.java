package com.side.project.domain.item;

import com.side.project.domain.item.itemdto.ItemDto;
import com.side.project.domain.item.itemdto.ItemSaveDto;
import com.side.project.domain.item.itemdto.ItemUpdateDto;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.config.TestcontainersConfig;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.member.Role;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.login.UnauthorizedException;
import com.side.project.web.exception.member.DuplicateMemberException;
import com.side.project.web.login.LoginMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfig.class)
@SpringBootTest
@Transactional
class ItemServiceTest {

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

    private Member createAndSaveMember(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member(prefix + suffix, "홍길동", "password123", prefix + "Nick" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호"));
        return memberRepository.save(member);
    }

    private Item createAndSaveItem(Member seller, ItemStatus status) {
        Item item = new Item("상품명" + UUID.randomUUID(), "상품설명", 10000, status, Category.TOP, seller);
        item.addItemImage(new ItemImage("original.png", "stored.png"));
        return itemRepository.save(item);
    }

    @Test
    void save_성공_파일없음() throws IOException {
        Member seller = createAndSaveMember("seller");
        ItemSaveDto saveDto = new ItemSaveDto();
        saveDto.setName("새상품");
        saveDto.setDescription("상품설명");
        saveDto.setPrice(10000);
        saveDto.setCategory(Category.TOP);

        Long savedId = itemService.save(saveDto, seller.getLoginId(), null);

        Item saved = itemRepository.findById(savedId).orElseThrow();
        assertThat(saved.getName()).isEqualTo("새상품");
        assertThat(saved.getSeller().getId()).isEqualTo(seller.getId());
    }

    @Test
    void save_성공_파일포함() throws IOException {
        Member seller = createAndSaveMember("seller");
        ItemSaveDto saveDto = new ItemSaveDto();
        saveDto.setName("새상품");
        saveDto.setDescription("상품설명");
        saveDto.setPrice(10000);
        saveDto.setCategory(Category.TOP);
        MultipartFile multipartFile = new MockMultipartFile("file", "original.png", "image/png",
                "dummy-image-content".getBytes(StandardCharsets.UTF_8));

        Long savedId = itemService.save(saveDto, seller.getLoginId(), List.of(multipartFile));

        Item saved = itemRepository.findByIdWithMember(savedId).orElseThrow();
        assertThat(saved.getItemImages()).hasSize(1);
        assertThat(saved.getThumbnailImage()).isNotNull();
        assertThat(saved.getThumbnailImage().getOriginalFilename()).isEqualTo("original.png");
    }

    @Test
    void save_존재하지않는_회원() {
        ItemSaveDto saveDto = new ItemSaveDto();
        saveDto.setName("새상품");
        saveDto.setPrice(10000);

        assertThatThrownBy(() -> itemService.save(saveDto, "no-such-login-id-" + UUID.randomUUID(), null))
                .isInstanceOf(DuplicateMemberException.class);
    }

    @Test
    void findByIdToDto_성공() {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);

        ItemDto result = itemService.findByIdToDto(item.getId());

        assertThat(result.getItemId()).isEqualTo(item.getId());
        assertThat(result.getName()).isEqualTo(item.getName());
    }

    @Test
    void findByIdToDto_존재하지않는_상품() {
        assertThatThrownBy(() -> itemService.findByIdToDto(-1L))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_성공() {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        LoginMember loginMember = new LoginMember(seller.getId(), seller.getLoginId(), seller.getNickName(), seller.getRole());

        itemService.delete(item.getId(), loginMember);

        assertThat(itemRepository.findById(item.getId())).isEmpty();
    }

    @Test
    void delete_존재하지않는_상품() {
        Member seller = createAndSaveMember("seller");
        LoginMember loginMember = new LoginMember(seller.getId(), seller.getLoginId(), seller.getNickName(), seller.getRole());

        assertThatThrownBy(() -> itemService.delete(-1L, loginMember))
                .isInstanceOf(ItemException.class);
    }

    @Test
    void delete_권한없음() {
        Member seller = createAndSaveMember("seller");
        Member other = createAndSaveMember("other");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        LoginMember loginMember = new LoginMember(other.getId(), other.getLoginId(), other.getNickName(), other.getRole());

        assertThatThrownBy(() -> itemService.delete(item.getId(), loginMember))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void update_성공() throws IOException {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setName("수정된상품명");

        ItemDto result = itemService.update(item.getId(), updateDto, null, seller.getLoginId());

        assertThat(result.getName()).isEqualTo("수정된상품명");
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getName()).isEqualTo("수정된상품명");
    }

    @Test
    void update_존재하지않는_상품() {
        Member seller = createAndSaveMember("seller");
        ItemUpdateDto updateDto = new ItemUpdateDto();

        assertThatThrownBy(() -> itemService.update(-1L, updateDto, null, seller.getLoginId()))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void update_권한없음() {
        Member seller = createAndSaveMember("seller");
        Member other = createAndSaveMember("other");
        Item item = createAndSaveItem(seller, ItemStatus.SELLING);
        ItemUpdateDto updateDto = new ItemUpdateDto();

        assertThatThrownBy(() -> itemService.update(item.getId(), updateDto, null, other.getLoginId()))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
