# Wishlist API (`/api/wishlist/**`) — 위시리스트

소스: `domain/wishlist/WishListController.java`, `WishListService.java`, `domain/wishlist/wishlistdto/*`, `frontend/src/api/client.js`의 `listWishlist/addWishlist/removeWishlist/findWished`(및 mock 모드 로직).

`CLAUDE.md`에는 예전에 "위시리스트는 백엔드에 존재하지 않습니다"라고 적혀 있었지만, 실제로는 목록 조회 / 찜 추가 / 찜 해제 / 찜 여부 단건 조회가 **모두 구현되어 있습니다.** 프론트는 백엔드가 실제로 내려주는 응답 형태·필드명을 그대로 받아서 처리하도록 맞춰져 있습니다(아래 각 엔드포인트 참고).

---

### GET /api/wishlist

**상태**: 구현됨

**요청**
- 인증 필요. Query(옵션): `page`(기본 0), `size`(기본 10).

**성공 응답 (실제 응답 — `WishListPageResponseDto`)**
```json
{
  "list": [
    {
      "wishListId": 1,
      "itemId": 10,
      "itemName": "item1",
      "description": "desc",
      "price": 1100,
      "itemStatus": "SELLING",
      "sellerNickName": "seller1",
      "thumbnailFilename": "stored-item1-image1.jpg"
    }
  ],
  "hasNext": false
}
```

프론트는 이 형태를 그대로 받아 처리합니다:
- `client.js`의 `listWishlist(page, size)`는 `GET /api/wishlist?page=&size=`를 호출합니다(mock 모드도 동일하게 `{list, hasNext}` 형태로 응답).
- `Wishlist.jsx`는 응답에서 `data.list`를 꺼내 배열로 순회하고(과거 응답이 배열 그대로 오는 경우를 대비해 `Array.isArray(data)` 폴백도 남겨둠), 각 항목을 `normalizeItem`에 넘겨 렌더링하며, 각 카드에 찜 해제 버튼을 함께 표시합니다.
- `normalize.js`의 `normalizeItem`이 `itemName`→`name`, `itemStatus`→`status`, `sellerNickName`→`nickName` 필드명 차이를 흡수합니다(기존 `items`/`orders` 도메인이 쓰는 `name`/`status`/`nickName` 필드도 계속 지원하는 폴백 체인이라 다른 화면에는 영향 없음).
- 이 DTO에는 `memberId`(판매자 회원 ID)가 없어서, 위시리스트 카드에서는 판매자 상점(`/shop/{memberId}`)으로 이동하는 링크를 만들 수 없습니다. 필요해지면 백엔드 DTO에 `memberId`(또는 `sellerId`) 필드 추가가 선행되어야 합니다.

**에러 응답**

| HTTP 상태 | 발생 조건 |
|---|---|
| 401 | 세션 없음 |

---

### GET /api/wishlist/{itemId} (찜 여부 단건 조회)

**상태**: 구현됨

상품 상세 페이지(`Detail.jsx`)가 진입 시 이 API로 현재 로그인 회원이 해당 상품을 이미 찜했는지 조회해, 하트 아이콘의 초기 상태를 새로고침 후에도 정확히 표시합니다.

**요청**
- 인증 필요. Path: `itemId`.

**성공 응답 (실제 응답 — `WishListController.findItemWished`)**
```json
{ "itemId": "10", "wished": "true" }
```
⚠️ 값이 문자열(`Map<String,String>`)로 내려옵니다. 프론트는 `result?.wished === true || result?.wished === "true"`처럼 두 형태 모두 허용해서 비교합니다.

**에러 응답**

| HTTP 상태 | 발생 조건 |
|---|---|
| 401 | 세션 없음 |

---

### POST /api/wishlist/{itemId} (찜 추가)

**상태**: 구현됨

**요청**
- 인증 필요. Path: `itemId`. Body 없음.

**성공 응답 (실제 응답)**
```json
{ "itemId": "10", "wished": "true" }
```

**에러 응답**

| HTTP 상태 | 메시지 예시 | 발생 조건 |
|---|---|---|
| 401 | - | 세션 없음 |
| 404 | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |
| 409 | "이미 찜한 상품입니다." | 이미 찜한 상품에 다시 POST(`WishListService.addWishList`가 `existsByItemIdAndMemberId`로 사전 검사 후 `WishListException` 발생) |

---

### DELETE /api/wishlist/{itemId} (찜 해제)

**상태**: 구현됨

**요청**
- 인증 필요. Path: `itemId`. Body 없음.

**성공 응답**
```json
{ "itemId": "10", "wished": "false" }
```

**에러 응답**

| HTTP 상태 | 메시지 예시 | 발생 조건 |
|---|---|---|
| 401 | - | 세션 없음 |
| 404 | "찜한 상품을 찾을 수 없습니다." | 애초에 찜한 적 없는 상품을 삭제 시도(`WishListNotFoundException`) |

`frontend/src/api/client.js`의 `removeWishlist(itemId)`가 이 API를 호출합니다. `Detail.jsx`의 찜 버튼은 이제 단방향 추가가 아니라 **토글**(`wished`이면 `removeWishlist`, 아니면 `addWishlist`)이고, `Wishlist.jsx`의 각 항목에도 찜 해제 버튼이 있습니다.
