# Items API (`/api/items/**`)

소스: `domain/item/ItemController.java`, `ItemService.java`, `domain/item/itemdto/*`, `frontend/src/api/client.js`의 `listItems/findItem/createItem/updateItem/deleteItem`

---

### GET /api/items

**상태**: 구현됨

**요청**
- 인증 불필요.
- Query(전부 옵션): `page`(기본 0), `size`(기본 10), `keyword`, `category`(`OUTER`/`TOP`/`BOTTOM`/`BAG`/`SHOES`/`ACCESSORY`/`ETC`), `status`(`SELLING`/`RESERVED`/`SOLD`), `priceGoe`, `priceLoe`
- 예: `GET /api/items?page=0&size=10&keyword=nike&category=SHOES&priceGoe=10000&priceLoe=50000`

**성공 응답**
```json
{
  "list": [
    {
      "itemId": 1,
      "memberId": 1,
      "name": "item1",
      "description": "description1",
      "price": 1100,
      "status": "SELLING",
      "category": "OUTER",
      "nickName": "nickname1",
      "thumbnailFilename": "stored-item1-image1.jpg"
    }
  ],
  "hasNext": true
}
```
목록 항목에는 이미지가 `thumbnailFilename` 문자열 하나만 있고, 상세 조회처럼 `itemImages` 배열은 없습니다. 이미지를 표시하려면 `/api/images/{thumbnailFilename}`로 직접 URL을 조립하세요.

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 400 | `invalid_request` | "가격 조건이 올바르지 않습니다." | `priceGoe` > `priceLoe` 등 잘못된 파라미터 |

---

### GET /api/items/{itemId}

**상태**: 구현됨

**요청**
- 인증 불필요. Path: `itemId`.

**성공 응답**
```json
{
  "itemId": 1,
  "memberId": 1,
  "name": "item1",
  "description": "description1",
  "price": 1100,
  "status": "SELLING",
  "category": "OUTER",
  "nickName": "nickname1",
  "itemImages": [
    { "itemImageId": 1, "originalFilename": "item1-image1.jpg", "storedFileName": "stored-item1-image1.jpg" }
  ]
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 404 | `not_found_item` | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |

---

### POST /api/items (상품 등록)

**상태**: 구현됨

**요청**
- 인증 필요.
- `Content-Type: multipart/form-data`, 파트 2개:
  - `itemSaveDto` (`application/json`):
    ```json
    { "name": "New Item", "description": "description", "price": 10000, "category": "OUTER" }
    ```
    `name`/`price` 필수.
  - `multipartFiles`: 이미지 파일 1장 이상 **필수**(파트 자체가 없으면 400 — 썸네일이 항상 있어야 하기 때문).

**성공 응답** (201 Created)
```json
{
  "itemId": 15,
  "name": "New Item",
  "description": "description",
  "price": 10000,
  "category": "OUTER"
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 400 | `invalid_request` | "상품명을 입력해주세요." | `name`/`price` 누락, 이미지 파일 0장 |

---

### PATCH /api/items/{itemId} (상품 수정)

**상태**: 구현됨

**요청**
- 인증 필요(소유자만 — 검증은 서비스 내부에서 수행).
- `Content-Type: multipart/form-data`, 파트 2개:
  - `itemUpdateDto` (`application/json`):
    ```json
    { "name": "Updated Item", "description": "updated", "price": 12000, "status": "RESERVED", "category": "OUTER", "deletedFileIds": [1, 2] }
    ```
  - `multipartFiles`: 옵션(새로 추가할 이미지가 있을 때만).

**성공 응답**
```json
{
  "itemId": 1,
  "memberId": 1,
  "name": "Updated Item",
  "description": "updated",
  "price": 12000,
  "status": "RESERVED",
  "category": "OUTER",
  "nickName": "nickname1",
  "itemImages": []
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 403 | `forbidden` | "본인이 등록한 상품만 수정할 수 있습니다." | 소유자가 아닌 회원이 수정 시도 |
| 404 | `not_found_item` | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |

⚠️ **알려진 이슈**: 요청 바디의 `status` 필드가 그대로 반영되므로, 상품 소유자가 `orders.md`의 주문 흐름과 무관하게 `SOLD`/`RESERVED`를 임의로 바꿀 수 있습니다. `orders.md`의 "주문 상태전이" 기능이 도입되면, 이 엔드포인트에서 `status` 필드는 제거하고 상태 변경은 오직 주문 상태전이 API를 통해서만 가능하도록 막는 것을 제안합니다(어길 시 `409 conflict_state`).

---

### DELETE /api/items/{itemId}

**상태**: 구현됨

**요청**
- 인증 필요(소유자만). Path: `itemId`.

**성공 응답**
- HTTP 204, 바디 없음(프론트는 204 응답의 바디를 읽지 않도록 이미 처리되어 있습니다).
- ⚠️ 현재 실제로는 204인데도 문자열 바디(`"삭제 완료"`)를 담아 보냅니다. 204는 원래 바디가 없어야 하는 상태 코드이므로, 백엔드에서 바디 없이 `204`만 반환하도록 정리하는 것을 제안합니다.

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 403 | `forbidden` | "본인이 등록한 상품만 삭제할 수 있습니다." | 소유자가 아닌 회원이 삭제 시도 |
| 404 | `not_found_item` | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |
