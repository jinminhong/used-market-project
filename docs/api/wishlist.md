# Wishlist API (`/api/wishlist/**`) — 위시리스트

소스: `domain/wishlist/WishListController.java`, `WishListService.java`, `domain/wishlist/wishlistdto/*`, `frontend/src/api/client.js`의 `listWishlist/addWishlist/removeWishlist/findWished`.

코드리뷰(개선 방향)는 [`docs/wishlist-review.md`](../wishlist-review.md) 참고.

목록 조회 / 찜 여부 조회 / 찜 추가 / 찜 해제가 모두 구현되어 있다.

---

### GET /api/wishlist

**상태**: 구현됨. 인증 필요. `page`/`size` 페이징으로 `{ list, hasNext }` 반환. 항목에 `sellerNickName`은 있지만 판매자 `memberId`는 없어 상점 링크를 만들 수 없다(상세: `wishlist-review.md`).

---

### GET /api/wishlist/{itemId} (찜 여부 단건 조회)

**상태**: 구현됨. 인증 필요. `{ itemId, wished }` 반환.

⚠️ 값이 `Map<String,String>`이라 `wished`가 문자열(`"true"`)로 내려온다.

---

### POST /api/wishlist/{itemId} (찜 추가)

**상태**: 구현됨. 인증 필요. 이미 찜한 상품이면 409.

---

### DELETE /api/wishlist/{itemId} (찜 해제)

**상태**: 구현됨. 인증 필요. `Detail.jsx`의 찜 버튼은 토글 방식으로 이 API와 추가 API를 함께 사용한다. 찜한 적 없는 상품 삭제 시도는 404.
</content>
