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

**상태**: 미구현(확정 설계) — 상세 구현 방법은 [`docs/ORDER_LIFECYCLE_GUIDE.md`](../ORDER_LIFECYCLE_GUIDE.md) 참고. 프론트(`frontend/src/api/client.js`의 `changeOrderStatus`, `SalesHistory.jsx`/`PurchaseHistory.jsx`)는 이미 이 계약을 전제로 구현되어 있습니다.

목표: `OrderStatus`에 이미 정의만 되어 있고 실제로는 도달 불가능한 `REQUESTED`/`ACCEPTED`/`CANCELED`를 실제로 쓸 수 있게 하고, 결제/배송 단계(`PAY_COMPLETED`→`SHIPPING`→`COMPLETED`)를 추가합니다. 기존 즉시구매(`POST /api/orders/{itemId}`)는 프론트(`Checkout.jsx`)가 이미 사용 중이므로 하위 호환을 유지하며 그대로 `PAY_COMPLETED`로 직행하고, `SHIP`/`CONFIRM` 단계부터 네고 흐름과 합류합니다.

**요청**
- 인증 필요. Path: `orderId`.
```json
{ "action": "ACCEPT" }
```
`action` 후보 5종:

| action | 의미 | 허용 상태(From) | 결과(To) | 수행 주체 |
|---|---|---|---|---|
| `ACCEPT` | 판매자 승인 | `REQUESTED` | `ACCEPTED` | 판매자만 |
| `PAY` | 결제(시뮬레이션) | `ACCEPTED` | `PAY_COMPLETED` | 구매자만 |
| `SHIP` | 발송 처리 | `PAY_COMPLETED` | `SHIPPING` | 판매자만 |
| `CONFIRM` | 구매확정 | `SHIPPING` | `COMPLETED`(`Item.status`→`SOLD`) | 구매자만 |
| `CANCEL` | 취소 | `REQUESTED`/`ACCEPTED`: 누구든 자유취소, `PAY_COMPLETED`/`SHIPPING`: 판매자만(동의/귀책사유) | `CANCELED`(`Item.status`→`SELLING`) | 상태별로 다름 |

`COMPLETED`/`CANCELED`에서는 어떤 action도 거부됩니다(409).

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
| 403 | `forbidden` | "이 주문을 처리할 권한이 없습니다." / "판매자 동의가 필요합니다." | buyer/seller가 아닌 제3자, 또는 권한 없는 쪽이 액션 시도(예: 구매자가 `PAY_COMPLETED` 단계에서 임의 취소) |
| 404 | `not_found_order` | "주문을 찾을 수 없습니다." | 존재하지 않는 `orderId` |
| 409 | `conflict_state` | "잘못된 순서의 상태 변경입니다." | 상태표를 벗어난 역행/스킵 시도 |

---

## 관련 enum

- `ItemStatus`: SELLING / RESERVED / SOLD
- `OrderStatus`(확정): REQUESTED / ACCEPTED / CANCELED / PAY_COMPLETED / SHIPPING(신규) / COMPLETED (현재 코드는 `PAY_COMPLETED`만 실제로 사용)
- `Orders.agreedPrice`(신규 필드, 미구현): 협상가. 즉시구매는 `item.price`를 그대로 세팅.
