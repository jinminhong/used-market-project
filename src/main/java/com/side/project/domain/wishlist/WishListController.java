package com.side.project.domain.wishlist;

import com.side.project.domain.wishlist.wishlistdto.WishListPageResponseDto;
import com.side.project.web.argumentresolver.Login;
import com.side.project.web.login.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishListController {

    private final WishListService wishListService;

    @GetMapping("")
    public ResponseEntity<WishListPageResponseDto> wishList(@Login LoginMember loginMember,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(wishListService.getWishList(loginMember.getMemberId(), pageRequest));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Map<String, String>> findItemWished(@Login LoginMember loginMember,
                                                              @PathVariable("itemId") Long itemId) {
        Long memberId = loginMember.getMemberId();
        boolean wished = wishListService.existWishList(itemId, memberId);
        return ResponseEntity.ok(Map.of("itemId", "" + itemId, "wished", "" + wished));
    }

    @PostMapping("/{itemId}")
    public ResponseEntity<Map<String,String>> addWishList(@Login LoginMember loginMember,
                                         @PathVariable("itemId") Long itemId) {
        Long memberId = loginMember.getMemberId();
        wishListService.addWishList(itemId ,memberId);
        return ResponseEntity.ok(Map.of("itemId" , ""+itemId , "wished","true"));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Map<String,String>> deleteWishList(@Login LoginMember loginMember,
                                                             @PathVariable("itemId") Long itemId) {
        Long memberId = loginMember.getMemberId();
        wishListService.deleteWishList(itemId, memberId);
        return ResponseEntity.ok(Map.of("itemId", "" + itemId, "wished", "false"));
    }
}
