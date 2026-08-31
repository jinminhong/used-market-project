package com.side.project.domain.wishlist;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.wishlist.repository.WishListRepository;
import com.side.project.domain.wishlist.wishlistdto.WishListPageResponseDto;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.member.MemberException;
import com.side.project.web.exception.wishlist.WishListException;
import com.side.project.config.TestcontainersConfig;
import com.side.project.web.exception.wishlist.WishListNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfig.class)
@SpringBootTest
@Transactional
class WishListServiceTest {

    @Autowired
    private WishListService wishListService;

    @Autowired
    private WishListRepository wishListRepository;

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

    private Item createAndSaveItem(Member seller) {
        Item item = new Item("상품명" + UUID.randomUUID(), "설명", 10000, ItemStatus.SELLING, Category.TOP, seller);
        item.addItemImage(new ItemImage("original.png", "stored.png"));
        return itemRepository.save(item);
    }

    @Test
    void addWishList_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);

        wishListService.addWishList(item.getId(), buyer.getId());

        assertThat(wishListRepository.existsByItemIdAndMemberId(item.getId(), buyer.getId())).isTrue();
    }

    @Test
    void addWishList_이미_찜한상품() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        wishListService.addWishList(item.getId(), buyer.getId());

        assertThatThrownBy(() -> wishListService.addWishList(item.getId(), buyer.getId()))
                .isInstanceOf(WishListException.class);
    }

    @Test
    void addWishList_존재하지않는_상품() {
        Member buyer = createAndSaveMember("buyer");

        assertThatThrownBy(() -> wishListService.addWishList(-1L, buyer.getId()))
                .isInstanceOf(ItemException.class);
    }

    @Test
    void addWishList_존재하지않는_회원() {
        Member seller = createAndSaveMember("seller");
        Item item = createAndSaveItem(seller);

        assertThatThrownBy(() -> wishListService.addWishList(item.getId(), -1L))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void existWishList_위임확인() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        wishListService.addWishList(item.getId(), buyer.getId());

        boolean result = wishListService.existWishList(item.getId(), buyer.getId());

        assertThat(result).isTrue();
    }

    @Test
    void deleteWishList_성공() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        wishListService.addWishList(item.getId(), buyer.getId());

        wishListService.deleteWishList(item.getId(), buyer.getId());

        assertThat(wishListRepository.existsByItemIdAndMemberId(item.getId(), buyer.getId())).isFalse();
    }

    @Test
    void deleteWishList_존재하지않는_찜() {
        Member buyer = createAndSaveMember("buyer");

        assertThatThrownBy(() -> wishListService.deleteWishList(-1L, buyer.getId()))
                .isInstanceOf(WishListNotFoundException.class);
    }

    @Test
    void getWishList_위임확인() {
        Member seller = createAndSaveMember("seller");
        Member buyer = createAndSaveMember("buyer");
        Item item = createAndSaveItem(seller);
        wishListService.addWishList(item.getId(), buyer.getId());
        Pageable pageable = PageRequest.of(0, 10);

        WishListPageResponseDto result = wishListService.getWishList(buyer.getId(), pageable);

        assertThat(result.getList()).extracting("itemId").contains(item.getId());
        assertThat(result.isHasNext()).isFalse();
    }
}
