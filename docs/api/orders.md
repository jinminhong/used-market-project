# Orders API (`/api/orders/**`) — 구매/판매 내역

소스: `domain/orders/OrdersController.java`, `OrdersService.java`, `domain/orders/ordersdto/*`, `frontend/src/api/client.js`의 `buyItem/listPurchases/listSales`, 미구현 부분은 `docs/BACKEND_ROADMAP.txt`(단계 1)를 근거로 함.

`CLAUDE.md`에는 예전에 "주문/거래 시스템은 백엔드에 존재하지 않습니다"라고 적혀 있었지만, 실제로는 **즉시구매(1단계)까지는 구현되어 있고**, 판매자 승인/구매확정 같은 상태전이(2단계 이상)만 없는 상태입니다.

---

### POST /api/orders/{itemId} (즉시구매)

**상태**: 구현됨

**요청**
- 인증 필요. Path: `itemId`. Body 없음.

**성공 응답**
```json
{ "status": "ok", "message": "구매가 완료되었습니다." }
```
성공 시 서버 내부적으로 주문이 `PAY_COMPLETED` 상태로 즉시 생성되고, 상품 상태는 `RESERVED`로 바뀝니다.

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 404 | `not_found_item` | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |
| 409 | `conflict_state` | "구매할 수 없는 상품입니다." | 상품이 `SELLING` 상태가 아님(이미 예약/판매됨) |
| 409 | `conflict_state` | "본인이 등록한 상품은 구매할 수 없습니다." | 본인 상품을 구매 시도 |

---

### GET /api/orders/purchases

**상태**: 구현됨

**요청**
- 인증 필요. Query: `page`(기본 0), `size`(기본 10).

**성공 응답**
```json
{
  "list": [
    {
      "orderId": 1,
      "itemId": 10,
      "name": "item1",
      "description": "desc",
      "price": 1100,
      "status": "RESERVED",
      "sellerNickName": "seller1",
      "thumbnailFilename": "stored-item1-image1.jpg",
      "purchaseDate": "2026-07-19T10:00:00"
    }
  ],
  "hasNext": false
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |

---

### GET /api/orders/sales

**상태**: 구현됨

**요청**
- 인증 필요. Query: `page`(기본 0), `size`(기본 10).

**성공 응답**
- `GET /api/orders/purchases`와 동일한 구조(`list`의 각 항목이 `OrdersResponseDto`), 다만 내가 **판매자**인 주문만 필터링됩니다.

**에러 응답**
- `GET /api/orders/purchases`와 동일.

---

### PATCH /api/orders/{orderId} (주문 상태전이)

**상태**: 미구현(제안) — `docs/BACKEND_ROADMAP.txt` 단계 1 근거

목표: `OrderStatus`에 이미 정의만 되어 있고 실제로는 도달 불가능한 `REQUESTED`/`ACCEPTED`/`CANCELED`를 실제로 쓸 수 있게 하는 상태전이 API. 기존 즉시구매(`POST /api/orders/{itemId}`)는 프론트(`Checkout.jsx`)가 이미 사용 중이므로 하위 호환을 유지하고, 그 위에 이 API를 추가로 얹는 것을 제안합니다.

**요청**
- 인증 필요. Path: `orderId`.
```json
{ "action": "ACCEPT" }
```
`action` 후보: `"ACCEPT"`(판매자 승인, `REQUESTED`→`ACCEPTED`), `"COMPLETE"`(구매확정, `ACCEPTED`→`PAY_COMPLETED`/`COMPLETED`), `"CANCEL"`(구매자/판매자 누구든, 그 외 상태→`CANCELED`, 이때 `Item.status`는 `SELLING`으로 복귀).
- 요청자가 buyer/seller 중 해당 액션을 수행할 권한이 있는 쪽인지 서버가 검증해야 합니다.

**성공 응답**
```json
{
  "orderId": 1,
  "itemId": 10,
  "status": "ACCEPTED",
  "itemStatus": "RESERVED"
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 403 | `forbidden` | "이 주문을 처리할 권한이 없습니다." | buyer/seller가 아닌 제3자가 액션 시도 |
| 404 | `not_found_order` | "주문을 찾을 수 없습니다." | 존재하지 않는 `orderId` |
| 409 | `conflict_state` | "잘못된 순서의 상태 변경입니다." | 정해진 경로(`REQUESTED`→`ACCEPTED`→`PAY_COMPLETED`/`COMPLETED`, 언제든→`CANCELED`)를 벗어난 역행/스킵 시도 |

⚠️ **미결정 사항** (로드맵 원문에 결정 보류로 남아 있음): 기존 즉시구매(`POST /api/orders/{itemId}`)가 지금처럼 `PAY_COMPLETED`로 바로 끝날지, 아니면 이 API 도입 후 `REQUESTED`부터 시작하도록 바뀔지는 `Checkout.jsx`가 "즉시 완료"를 기대하고 있는 점을 고려해 구현 시점에 다시 확인해야 합니다.

---

## 관련 enum

- `ItemStatus`: SELLING / RESERVED / SOLD
- `OrderStatus`: REQUESTED / ACCEPTED / CANCELED / PAY_COMPLETED / COMPLETED (현재 코드는 `PAY_COMPLETED`만 실제로 사용)
