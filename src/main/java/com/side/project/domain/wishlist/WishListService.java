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

    @Transactional
    public void addWishList(Long itemId , Long memberId) {
        if (wishListRepository.existsByItemIdAndMemberId(itemId, memberId)) {
            return;
        }
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        wishListRepository.save(new WishList(item , member));
    }

    public WishListPageResponseDto getWishList(Long memberId, Pageable pageable) {
        Slice<WishListResponseDto> wishList = wishListRepository.findAllWishList(memberId, pageable);
        return new WishListPageResponseDto(wishList.getContent(), wishList.hasNext());
    }

//    @Transactional
//    public void removeWishList(Long itemId, Long memberId) {
//        wishListRepository.fi
//        wishListRepository.delete();
//    }
}
