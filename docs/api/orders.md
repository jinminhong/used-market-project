# Orders API (`/api/orders/**`) — 구매/판매 내역

소스: `domain/orders/OrdersController.java`, `OrdersService.java`, `domain/orders/ordersdto/*`, `frontend/src/api/client.js`의 `buyItem/listPurchases/listSales`.

코드리뷰(개선 방향)는 [`docs/orders-review.md`](../orders-review.md), 상태전이 상세 설계는 [`docs/ORDER_LIFECYCLE_GUIDE.md`](../ORDER_LIFECYCLE_GUIDE.md) 참고.

즉시구매(1단계)까지는 구현되어 있고, 판매자 승인/구매확정 같은 상태전이(2단계 이상)는 아직 없다.

---

### POST /api/orders/{itemId} (즉시구매)

**상태**: 구현됨. 인증 필요. 비관적 락으로 동시구매를 안전하게 막는다(`ItemRepository.findByIdWithMemberForUpdate`). 성공 시 주문이 `PAY_COMPLETED`로 즉시 생성되고 상품은 `RESERVED`가 된다. 이미 판매된 상품/본인 상품 구매 시도는 에러(⚠️ 문서상 409로 기대되나 실제로는 404 — 상세는 `orders-review.md`).

---

### GET /api/orders/purchases / GET /api/orders/sales

**상태**: 구현됨. 인증 필요. `page`/`size` 페이징으로 각각 내가 구매자/판매자인 주문 목록을 `{ list, hasNext }` 형태로 반환.

---

### PATCH /api/orders/{orderId} (주문 상태전이)

**상태**: 미구현(확정 설계). `REQUESTED`→`ACCEPTED`→`PAY_COMPLETED`→`SHIPPING`→`COMPLETED` 상태 머신과 취소 흐름의 상세 계약은 [`docs/ORDER_LIFECYCLE_GUIDE.md`](../ORDER_LIFECYCLE_GUIDE.md)에, 구현 착수 시 우선순위는 [`docs/orders-review.md`](../orders-review.md)의 "다음에 손대면 좋을 순서"에 정리되어 있다.

---

## 관련 enum

- `ItemStatus`: SELLING / RESERVED / SOLD
- `OrderStatus`(확정 설계): REQUESTED / ACCEPTED / CANCELED / PAY_COMPLETED / SHIPPING(신규) / COMPLETED — 현재 코드는 `PAY_COMPLETED`만 실제로 사용
</content>
