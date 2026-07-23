# Items API (`/api/items/**`)

소스: `domain/item/ItemController.java`, `ItemService.java`, `domain/item/itemdto/*`, `frontend/src/api/client.js`의 `listItems/findItem/createItem/updateItem/deleteItem`

코드리뷰(개선 방향)는 [`docs/items-review.md`](../items-review.md) 참고.

---

### GET /api/items

**상태**: 구현됨. 인증 불필요. `page`/`size`/`keyword`/`category`/`status`/`priceGoe`/`priceLoe` 쿼리로 QueryDSL 동적 검색, `{ list, hasNext }` Slice 응답. 목록 항목은 `thumbnailFilename` 문자열 하나만 포함(이미지 URL은 `/api/images/{그 값}`로 조립).

---

### GET /api/items/{itemId}

**상태**: 구현됨. 인증 불필요. 상세 정보 + `itemImages` 배열 반환. 없으면 404.

---

### POST /api/items (상품 등록)

**상태**: 구현됨. 인증 필요. `multipart/form-data`(`itemSaveDto` JSON + `multipartFiles` 1장 이상 필수)로 등록.

---

### PATCH /api/items/{itemId} (상품 수정)

**상태**: 구현됨. 인증 필요(소유자만), `multipart/form-data`로 부분 수정 + 이미지 추가/삭제(`deletedFileIds`).

⚠️ **알려진 이슈**: 요청 바디의 `status` 필드가 그대로 반영되어, 소유자가 주문 흐름과 무관하게 `SOLD`/`RESERVED`를 임의로 바꿀 수 있다. 또한 삭제된 이미지의 실제 파일이 디스크에서 지워지지 않는다. 상세: `items-review.md`.

---

### DELETE /api/items/{itemId}

**상태**: 구현됨. 인증 필요(소유자만). 204 반환(⚠️ 실제로는 문자열 바디 포함). 위시리스트/채팅방이 걸린 상품 삭제 시 FK 위반 가능성 등 상세는 `items-review.md` 참고.
</content>
