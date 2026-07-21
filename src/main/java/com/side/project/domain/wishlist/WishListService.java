package com.side.project.domain.wishlist;

import com.side.project.domain.item.Item;
import com.side.project.domain.item.repository.ItemRepository;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.domain.wishlist.repository.WishListRepository;
import com.side.project.domain.wishlist.wishlistdto.WishListPageResponseDto;
import com.side.project.domain.wishlist.wishlistdto.WishListResponseDto;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.member.MemberException;
import com.side.project.web.exception.wishlist.WishListException;
import com.side.project.web.exception.wishlist.WishListNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishListService {

    private final WishListRepository wishListRepository;
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;

    public WishListPageResponseDto getWishList(Long memberId, Pageable pageable) {
        Slice<WishListResponseDto> wishList = wishListRepository.findAllWishList(memberId, pageable);
        return new WishListPageResponseDto(wishList.getContent(), wishList.hasNext());
    }

    @Transactional
    public void addWishList(Long itemId , Long memberId) {
        if (existWishList(itemId, memberId)) throw new WishListException("이미 찜한 상품입니다.");
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        wishListRepository.save(new WishList(item , member));
    }

    public boolean existWishList(Long itemId, Long memberId) {
        return wishListRepository.existsByItemIdAndMemberId(itemId, memberId);
    }

    @Transactional
    public void deleteWishList(Long itemId, Long memberId) {
        WishList wishList = wishListRepository.findWishListByItemIdAndMemberId(itemId, memberId).orElseThrow(() -> new WishListNotFoundException("찜한 상품을 찾을 수 없습니다."));
        wishListRepository.delete(wishList);
    }
}
