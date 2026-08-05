# Orders API (`/api/orders/**`) — 구매/판매 내역, 상태전이, 운송장

소스: `domain/orders/OrdersController.java`, `OrdersService.java`, `domain/orders/ordersdto/*`, `domain/ordershistory/**`(내부 감사로그), `frontend/src/api/client.js`의 `buyItem/listPurchases/listSales/changeOrderStatus/saveTracking`.

코드리뷰(개선 방향)는 [`docs/orders-review.md`](../orders-review.md) 참고.

즉시구매, 채팅 가격제안 연동, 주문 상태전이(PATCH), 운송장 등록까지 전부 구현되어 있다.

---

### POST /api/orders/{itemId} (즉시구매)

**상태**: 구현됨. 인증 필요. 비관적 락으로 동시구매를 안전하게 막는다(`ItemRepository.findByIdWithMemberForUpdate`). 성공 시 주문이 `PAY_COMPLETED`로 즉시 생성되고 상품은 `RESERVED`가 된다. 이미 판매된 상품은 409(CONFLICT), 본인 상품 구매 시도는 403(FORBIDDEN)으로 응답한다 — `ItemException`이 `HttpStatus` 필드를 갖도록 리팩터링되어 더 이상 404로 고정 매핑되지 않는다(상세는 [`docs/orders-review.md`](../orders-review.md) "해결됨" 섹션 참고). 락 획득 실패(`PessimisticLockingFailureException`)는 409로 매핑된다.

---

### PATCH /api/orders/{orderId} (주문 상태전이)

**상태**: 구현됨(`OrdersController.java:30-36` → `OrdersService.changeStatus`). 인증 필요. 요청 바디 `{ "action": "ACCEPT" | "PAY" | "SHIP" | "CONFIRM" | "CANCEL" }`, 응답 `OrdersActionResponseDto { orderId, itemId, status(OrderStatus), itemStatus(ItemStatus) }`.

| action | 조건(from) | 주체 | 결과(to) | Item.status |
|---|---|---|---|---|
| `ACCEPT` | `REQUESTED` | 판매자만 | `ACCEPTED` | 유지 |
| `PAY` | `ACCEPTED` | 구매자만 | `PAY_COMPLETED` | 유지 |
| `SHIP` | `PAY_COMPLETED` | 판매자만 | `SHIPPING` | 유지 |
| `CONFIRM` | `SHIPPING` | 구매자만 | `COMPLETED` | `RESERVED→SOLD` |
| `CANCEL` | `REQUESTED`/`ACCEPTED` | 구매자 또는 판매자 | `CANCELED` | `RESERVED→SELLING` |
| `CANCEL` | `PAY_COMPLETED`/`SHIPPING` | **판매자만**(동의 필요) | `CANCELED` | `RESERVED→SELLING` |
| `CANCEL` | `COMPLETED`/`CANCELED` | - | 409 | - |
| 그 외 역행/스킵/알 수 없는 action | - | - | 409 | - |

정해진 순서를 벗어난 상태 요청은 `OrdersException(HttpStatus.CONFLICT)`(409), 권한 없는 주체의 요청은 `OrdersException(HttpStatus.FORBIDDEN)`(403), 주문을 찾을 수 없으면 `OrdersException(HttpStatus.NOT_FOUND)`(404)를 던진다. 상태가 바뀔 때마다 `domain/ordershistory`(`OrdersHistoryService.save`)에 스냅샷 1건이 append된다(조회 API는 없음, 내부 감사로그 용도).

**채팅 가격제안과의 관계**: `REQUESTED` 주문은 이 PATCH가 아니라 채팅 가격제안 "생성" 시점에 이미 만들어진다(`ChatRoomService.createOffer` → `OrdersService.createNegotiationOrder`, 상세는 [`docs/api/chat.md`](chat.md)). 판매자가 채팅에서 제안을 수락하면 `ChatRoomService.acceptOffer` → `OrdersService.acceptNegotiation`이 같은 `REQUESTED` 주문을 `ACCEPTED`로 전이시키며, 이는 위 표의 `ACCEPT` 액션과 동일한 내부 로직(`applyAccept`)을 공유한다. 즉 `REQUESTED→ACCEPTED` 전이는 이 PATCH를 직접 호출하거나(현재 프론트에서는 사용 안 함) 채팅 수락을 통해서만 일어난다.

---

### PATCH /api/orders/{orderId}/tracking (운송장 등록)

**상태**: 구현됨(`OrdersController.java:58-64` → `OrdersService.registerTracking`). 인증 필요(판매자 본인만, 불일치 시 `OrdersException(FORBIDDEN)` 403). 요청 바디 `TrackingUpdateDto { trackingCompany, trackingNumber }`(둘 다 `@NotEmpty`, 누락 시 400). 응답은 `OrdersResponseDto`(아래 참고).

⚠️ **알려진 이슈**: 주문의 `orderStatus`가 어떤 값이어도(예: `REQUESTED` 단계에서도) 등록이 그대로 통과한다 — 상태 검증이 없다. 외부 택배사 API(SweetTracker 등) 연동은 없고, 판매자가 입력한 택배사명/운송장번호 문자열 두 개를 그대로 저장할 뿐이다(실시간 배송 조회 기능은 미구현, 상세는 `orders-review.md`).

---

### GET /api/orders/purchases / GET /api/orders/sales

**상태**: 구현됨. 인증 필요. `page`/`size` 페이징으로 각각 내가 구매자/판매자인 주문 목록을 `{ list, hasNext }` 형태로 반환. 목록 항목(`OrdersResponseDto`)에는 `orderId, itemId, name, description, price, status(ItemStatus), orderStatus(OrderStatus), sellerNickName, thumbnailFilename, trackingCompany, trackingNumber, purchaseDate, agreedPrice`가 포함된다.

**쿼리 파라미터**: `status`는 `OrderStatus` 값을 **다중 지정**할 수 있다(`@RequestParam(name="status") List<OrderStatus> statuses`) — 예: `GET /api/orders/purchases?status=REQUESTED&status=ACCEPTED&page=0&size=10`. 파라미터를 아예 생략하면 필터링 없이 **모든 상태의 주문이 다 조회된다**(`OrdersRepositoryImpl.orderStatusIn`이 `statuses`가 비어있으면 `null` 조건을 반환) — 즉 `CANCELED`/`REJECTED`로 끝난 주문까지 기본 목록에 섞여 나온다(알려진 제약, [`docs/orders-review.md`](../orders-review.md) 참고).

**프론트 연동**: `frontend/src/pages/PurchaseHistory.jsx`가 `TAB_STATUSES`로 탭마다 `status`를 항상 명시적으로 지정해 호출한다 — `구매완료`(`status=PAY_COMPLETED&status=SHIPPING&status=COMPLETED`) / `제안 중인 내역`(`status=REQUESTED`) / `합의된 내역`(`status=ACCEPTED`). 파라미터 생략 시의 무필터 동작(위 문단)은 백엔드에는 여전히 남아 있지만 프론트가 항상 명시적으로 지정하도록 고쳐져 실사용에서는 더 이상 노출되지 않는다(상세는 [`docs/orders-review.md`](../orders-review.md) "해결됨" 섹션 참고). `frontend/src/pages/SalesHistory.jsx`의 "받은 제안" 탭도 `status=ACCEPTED`로 호출하며, "발송 대기"(`status=PAY_COMPLETED`) 탭에서 `ShippingManagement.jsx`로 이동해 운송장을 등록한다(`ShippingForm.jsx` → `saveTracking`).

---

## 관련 enum

- `ItemStatus`: `SELLING` / `RESERVED` / `SOLD`
- `OrderStatus`: `REQUESTED` / `REJECTED` / `ACCEPTED` / `CANCELED` / `PAY_COMPLETED` / `SHIPPING` / `COMPLETED`(7종). 즉시구매(`OrdersService.save`)는 `PAY_COMPLETED`로 생성, 채팅 가격제안 생성(`createNegotiationOrder`)은 `REQUESTED`로 생성. 그 이후의 모든 전이는 위 PATCH 표를 따른다.

## 내부 감사로그: `domain/ordershistory`

`OrdersHistory` 엔티티(`orders` 참조 + `status` 스냅샷)와 `OrdersHistoryService.save(Orders)`로 구성된다. `changeStatus`/`acceptNegotiation` 등 상태가 바뀌는 모든 지점에서 호출되어 상태 변경 이력을 append-only로 쌓는다. **컨트롤러/조회 API가 없어 외부에 노출되지 않는 내부 전용 메커니즘**이며, 별도 도메인 API 문서를 두지 않는다.

## 자동취소 스케줄러 (미완성)

`OrdersAutoCancelScheduler`가 `@Scheduled(cron = "0 0 * * * *")`로 매시 정각 실행되지만, 현재는 로그만 남기고 실제 취소 로직(`PAY_COMPLETED` 상태로 오래 방치된 주문을 `CANCELED` + `Item.SELLING`으로 되돌리는 것)은 구현되어 있지 않다. `orders.auto-cancel.days` 프로퍼티(기본 2)만 읽어들인 상태.
