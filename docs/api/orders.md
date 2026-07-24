# Orders API (`/api/orders/**`) — 구매/판매 내역

소스: `domain/orders/OrdersController.java`, `OrdersService.java`, `domain/orders/ordersdto/*`, `frontend/src/api/client.js`의 `buyItem/listPurchases/listSales/changeOrderStatus`.

코드리뷰(개선 방향)는 [`docs/orders-review.md`](../orders-review.md), 상태전이 상세 설계는 [`docs/ORDER_LIFECYCLE_GUIDE.md`](../ORDER_LIFECYCLE_GUIDE.md) 참고.

즉시구매(`PAY_COMPLETED` 생성)와 채팅 가격제안 수락을 통한 주문 생성(`ACCEPTED` 생성, [`docs/api/chat.md`](chat.md) 참고)까지는 구현되어 있다. 생성 이후의 상태전이(판매자 승인/결제/발송/구매확정/취소 — `PATCH /api/orders/{orderId}`)는 아직 백엔드에 없고, 프론트만 이 계약을 전제로 먼저 구현되어 있다.

---

### POST /api/orders/{itemId} (즉시구매)

**상태**: 구현됨. 인증 필요. 비관적 락으로 동시구매를 안전하게 막는다(`ItemRepository.findByIdWithMemberForUpdate`). 성공 시 주문이 `PAY_COMPLETED`로 즉시 생성되고 상품은 `RESERVED`가 된다. 이미 판매된 상품/본인 상품 구매 시도는 에러(⚠️ 문서상 409로 기대되나 실제로는 404 — 상세는 `orders-review.md`).

---

### GET /api/orders/purchases / GET /api/orders/sales

**상태**: 구현됨. 인증 필요. `page`/`size` 페이징으로 각각 내가 구매자/판매자인 주문 목록을 `{ list, hasNext }` 형태로 반환.

**쿼리 파라미터**: `status`는 `OrderStatus` 값을 **다중 지정**할 수 있다(`@RequestParam(name="status") List<OrderStatus> statuses`, `OrdersController.java:35,45`) — 예: `GET /api/orders/purchases?status=REQUESTED&status=ACCEPTED&page=0&size=10`. 파라미터를 아예 생략하면 `OrdersRepositoryImpl`이 `orderStatus.in(COMPLETED, PAY_COMPLETED)`로 기본 필터링한다(`OrdersRepositoryImpl.java:73` 부근) — 즉 `REQUESTED`/`ACCEPTED` 상태 주문은 `status` 파라미터를 명시해야만 조회된다(알려진 제약, [`docs/orders-review.md`](../orders-review.md) 7번 참고).

**프론트 연동**: `frontend/src/pages/PurchaseHistory.jsx`가 구매내역을 `구매완료`(파라미터 없음, 기본 필터) / `제안 중인 내역`(`status=REQUESTED`) / `합의된 내역`(`status=ACCEPTED`) 3개 탭으로 나눠 이 쿼리 파라미터를 그대로 사용한다. `frontend/src/pages/SalesHistory.jsx`의 "받은 제안" 탭도 `status=ACCEPTED`로 동일하게 호출한다.

---

### PATCH /api/orders/{orderId} (주문 상태전이)

**상태**: 미구현(확정 설계). `REQUESTED`→`ACCEPTED`→`PAY_COMPLETED`→`SHIPPING`→`COMPLETED` 상태 머신과 취소 흐름의 상세 계약은 [`docs/ORDER_LIFECYCLE_GUIDE.md`](../ORDER_LIFECYCLE_GUIDE.md)에, 구현 착수 시 우선순위는 [`docs/orders-review.md`](../orders-review.md)의 "다음에 손대면 좋을 순서"에 정리되어 있다.

**프론트는 이미 이 계약을 전제로 구현되어 있다**: `frontend/src/api/client.js`의 `changeOrderStatus(orderId, action)`(`PATCH /api/orders/{orderId}`, body `{action}`)를 통해 `frontend/src/pages/OrderCheckout.jsx`("합의된 내역"에서 "구매하기"로 랜딩한 후 `action:"PAY"`), `PurchaseHistory.jsx`(구매자의 `CANCEL`/`CONFIRM`), `SalesHistory.jsx`(판매자의 `ACCEPT`/`CANCEL`/`SHIP`)가 이미 호출한다. mock 모드(`useMock=true`)에서는 `client.js`가 이 액션들을 자체 시뮬레이션하므로 백엔드 없이도 프론트 흐름을 확인할 수 있지만, 실제 백엔드에는 아직 해당 엔드포인트가 없어 실서버 연동 시 404가 난다.

---

## 관련 enum

- `ItemStatus`: SELLING / RESERVED / SOLD
- `OrderStatus`: REQUESTED / ACCEPTED / CANCELED / PAY_COMPLETED / COMPLETED (`SHIPPING`은 아직 미추가 — 확정 설계상으로만 존재, [`docs/ORDER_LIFECYCLE_GUIDE.md`](../ORDER_LIFECYCLE_GUIDE.md) 1절 참고). 즉시구매(`OrdersService.save`)는 `PAY_COMPLETED`로, 채팅 가격제안 수락(`ChatRoomService.acceptOffer`)은 `ACCEPTED`로 주문을 생성한다 — 둘 다 실제로 쓰인다. `REQUESTED`/`SHIPPING`/`COMPLETED`로의 전이는 위 PATCH 엔드포인트가 구현되어야 발생한다.
</content>
