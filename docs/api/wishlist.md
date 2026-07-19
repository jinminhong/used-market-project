# Wishlist API (`/api/wishlist/**`) — 위시리스트

소스: `domain/wishlist/WishListController.java`, `WishListService.java`, `domain/wishlist/wishlistdto/*`, `frontend/src/api/client.js`의 `listWishlist/addWishlist`(및 mock 모드 로직), 미구현 부분은 `frontend/src/api/client.js`의 mock 로직 + `docs/BACKEND_ROADMAP.txt`(단계 2)를 근거로 함.

`CLAUDE.md`에는 예전에 "위시리스트는 백엔드에 존재하지 않습니다"라고 적혀 있었지만, 실제로는 목록 조회와 찜 추가까지는 구현되어 있습니다. **찜 해제(삭제)만 없습니다.** 프론트는 백엔드가 실제로 내려주는 응답 형태·필드명을 그대로 받아서 처리하도록 맞춰져 있습니다(아래 각 엔드포인트 참고).

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
- `Wishlist.jsx`는 응답에서 `data.list`를 꺼내 배열로 순회하고(과거 응답이 배열 그대로 오는 경우를 대비해 `Array.isArray(data)` 폴백도 남겨둠), 각 항목을 `normalizeItem`에 넘겨 렌더링합니다.
- `normalize.js`의 `normalizeItem`이 `itemName`→`name`, `itemStatus`→`status`, `sellerNickName`→`nickName` 필드명 차이를 흡수합니다(기존 `items`/`orders` 도메인이 쓰는 `name`/`status`/`nickName` 필드도 계속 지원하는 폴백 체인이라 다른 화면에는 영향 없음).
- 이 DTO에는 `memberId`(판매자 회원 ID)가 없어서, 위시리스트 카드에서는 판매자 상점(`/shop/{memberId}`)으로 이동하는 링크를 만들 수 없습니다. 필요해지면 백엔드 DTO에 `memberId`(또는 `sellerId`) 필드 추가가 선행되어야 합니다.

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |

---

### POST /api/wishlist/{itemId} (찜 추가)

**상태**: 구현됨

**요청**
- 인증 필요. Path: `itemId`. Body 없음.

**성공 응답 (실제 응답)**
```json
{ "addWishList": "add" }
```
이 엔드포인트는 "추가"만 하는 단방향 동작입니다(이미 찜한 상품이면 조용히 무시하고 같은 응답을 돌려줌 — `WishListService.addWishList`가 `existsByItemIdAndMemberId`로 먼저 걸러냄). 응답 바디에 `itemId`/`wished` 같은 갱신 정보가 없어서, 프론트(`client.js`의 `addWishlist`, `Detail.jsx`의 `handleAddWishlist`)는 서버 응답 값을 읽지 않고 호출이 성공하면 로컬 `wished` state를 `true`로 고정합니다. 찜 해제 기능이 없으므로 이미 찜한 상태에서는 버튼을 비활성화해 "찜 해제"가 되는 것처럼 보이지 않게 처리했습니다(아래 섹션 참고).

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 404 | `not_found_item` | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |
| 409 | `duplicate_wishlist` | "이미 찜한 상품입니다." | 이미 찜한 상품에 다시 POST(현재는 이 검사가 없어 DB unique 제약 위반으로 500이 날 수 있음 — 아래 참고) |

⚠️ **알려진 이슈**: `WishList` 테이블에는 `(member_id, item_id)` DB unique 제약이 걸려 있는데, 서비스 코드가 중복 여부를 사전 검사하지 않습니다. 같은 상품에 두 번째로 "찜하기"를 누르면 제약 위반 예외가 그대로 터져 500 Internal Server Error가 나갈 가능성이 높습니다. 위 `409 duplicate_wishlist`로 명시적으로 막는 것을 제안합니다.

---

### 찜 해제(삭제)

**상태**: 미구현(제안) — 아래 두 방식 중 하나를 선택해야 합니다.

현재 `WishListService`에는 삭제 로직(`removeWishList`)이 통째로 주석 처리되어 있고, 컨트롤러에도 삭제 엔드포인트가 없습니다. 이 기능이 백엔드에 추가되기 전까지, 프론트(`Detail.jsx`)는 "찜하기" 버튼을 단방향 추가 동작으로만 취급합니다 — 이미 찜한 상태(`wished === true`)에서는 버튼을 비활성화하고 라벨을 "찜 완료"로 고정해, 실제로는 불가능한 "찜 해제"를 클릭할 수 있는 것처럼 보이지 않게 했습니다. 아래 옵션 중 하나로 삭제 엔드포인트가 생기면, `Detail.jsx`의 `handleAddWishlist`/버튼 `disabled` 조건과 `client.js`의 `addWishlist`를 다시 토글 형태로 고쳐야 합니다.

#### 옵션 A: 전용 DELETE 엔드포인트 추가 (추천)

```
DELETE /api/wishlist/{itemId}
```

**요청**
- 인증 필요. Path: `itemId`. Body 없음.

**성공 응답**
```json
{ "itemId": 10, "wished": false }
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 404 | `not_found_item` | "찜한 상품을 찾을 수 없습니다." | 애초에 찜한 적 없는 상품을 삭제 시도 |

#### 옵션 B: 기존 POST를 토글로 바꾸기

```
POST /api/wishlist/{itemId}
```
을 "이미 찜한 상품이면 삭제, 아니면 추가"하는 토글로 바꾸고, 현재 찜 여부를 응답으로 알려줍니다(요청 바디는 그대로 없어도 되고, 서버가 DB를 조회해서 판단).

**성공 응답**
```json
{ "itemId": 10, "wished": false }
```
(찜 해제된 경우) 또는
```json
{ "itemId": 10, "wished": true }
```
(찜 추가된 경우)

이 방식은 새 엔드포인트가 필요 없다는 장점이 있지만, "추가"와 "삭제"가 같은 URL·메서드로 묶여 있어 REST 관례상 명확성이 떨어지고, 클라이언트가 결과를 확인하려면 응답의 `wished` 값을 반드시 읽어야 합니다(현재 프론트 `handleAddWishlist`는 단방향 추가만 가정하고 응답을 읽지 않으므로, 이 방식을 택하면 프론트도 응답의 `wished` 값을 반영하도록 함께 고쳐야 합니다).

**어느 쪽을 택하든** 공통 에러:

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 404 | `not_found_item` | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |

---

### GET /api/wishlist/{itemId} (찜 여부 단건 조회)

**상태**: 미구현(제안)

상품 상세 페이지(`Detail.jsx`)의 하트 아이콘이 페이지 진입 시 "이미 찜한 상품인지" 알 방법이 없어서 항상 `false`로 시작하고, 새로고침하면 초기화됩니다. 이를 해결하려면 아래 엔드포인트를 제안합니다.

**요청**
- 인증 필요. Path: `itemId`.

**성공 응답**
```json
{ "itemId": 10, "wished": true }
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 404 | `not_found_item` | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |
