# Orders(구매/판매) 백엔드 리뷰

`domain/orders/**`, `domain/item/Item.java`(상태 전이 관련), `web/exception/GlobalExceptionHandler.java`를 대상으로 한 코드 리뷰 기록. 기준 문서는 [`docs/api/orders.md`](api/orders.md)(즉시구매 계약 + 상태전이 미구현 제안)와 [`docs/ORDER_LIFECYCLE_GUIDE.md`](ORDER_LIFECYCLE_GUIDE.md)(상태전이 상세 설계)다. 현재 구현은 "즉시구매(1단계)"까지만 있고 `REQUESTED`/`ACCEPTED`/`CANCELED`/`SHIPPING`은 설계만 있는 상태다.

## 남은 이슈

### 1. `Item.updateItem`의 `status` 뒷문이 주문 흐름과 완전히 분리되어 있어 실제 이중판매 경로가 된다

`ItemUpdateDto.status`가 그대로 노출되어 있어, 판매자가 상품 수정 API로 `Orders`와 무관하게 `Item.status`를 임의로 바꿀 수 있다. 예를 들어 구매 완료로 `Item`이 `RESERVED`, `Orders`가 `PAY_COMPLETED`인 상태에서 판매자가 `status=SELLING`으로 되돌리면, `Orders` 레코드는 고아가 되고 두 번째 구매자가 같은 상품을 다시 구매할 수 있다. `ORDER_LIFECYCLE_GUIDE.md` 6절이 정확히 이 뒷문을 지적하고 있는데 아직 코드에 남아 있다.

### 2. 락 타임아웃 예외가 처리되지 않는다

`ItemRepository.findByIdWithMemberForUpdate`는 `PESSIMISTIC_WRITE` + 3000ms 타임아웃으로 보호되어 있지만, 락을 3초 내에 못 얻으면 `LockTimeoutException`/`PessimisticLockException`이 던져지고 `GlobalExceptionHandler`에 핸들러가 없어 500으로 샌다. 트래픽이 몰리는 "핫한 매물" 시나리오에서 노출될 문제다.

### 3. 주문 취소/환불 기능이 전혀 없어 한 번 구매하면 아이템이 사실상 영구 `RESERVED`가 된다

`OrderStatus`에 `CANCELED`/`COMPLETED`가 정의만 되어 있고 전이 메서드가 없다. 한 번 구매가 일어나면 아이템을 `SOLD`로 만들 방법도, `SELLING`으로 되돌릴 방법도 API상 없다. 유일한 우회로가 1번의 `Item` 수정 백도어라, 쓰면 `Orders` 레코드가 고아로 남는 모순 상태가 된다.

### 4. `docs/api/orders.md`가 명시한 409가 실제로는 404로 나간다

"구매할 수 없는 상품입니다"/"본인이 등록한 상품은 구매할 수 없습니다"를 문서는 409로 약속했지만, 실제로 `OrdersService.save()`는 두 경우 모두 `ItemException`을 던지고 이는 항상 404로 고정 매핑된다. `ItemException`이 상태를 담는 필드가 없는 단순 예외라 메시지별 분기가 불가능하다.

### 5. `Orders` 엔티티/리포지토리에 상태전이 구현에 필요한 최소한의 골격이 없다

- `Orders`는 `id`/`buyer`/`item`/`orderStatus` 4개 필드뿐이라 협상가(`agreedPrice`)를 저장할 곳이 없다.
- `accept()`/`pay()`/`ship()`/`confirm()`/`cancel()` 같은 상태 검증 포함 전이 메서드가 없다.
- `orderId` 기준 fetch-join 조회가 없다 — `Orders.buyer`/`Orders.item`이 둘 다 `LAZY`라서 plain `findById`로 권한 체크를 하면 추가 쿼리가 2~3번 더 나간다.
- 자동취소 스케줄러용 쿼리도, `@EnableScheduling`도 없다.

### 6. `OrderStatus`에 `SHIPPING`이 아직 없다

`PAY_COMPLETED→SHIPPING→COMPLETED` 흐름을 표현할 수 없다.

### 7. 목록 조회 필터가 상태전이 확장을 전제하지 않은 하드코딩이다

`findAllPurchases`/`findAllSales`가 `orderStatus.in(COMPLETED, PAY_COMPLETED)`로 고정되어 있어, 상태전이가 추가되면 `REQUESTED`/`ACCEPTED`/`SHIPPING` 주문이 목록에서 아예 누락된다.

## 잘 되어 있는 부분

- **즉시구매 동시성은 실제로 안전하다.** `OrdersService.save()`가 `findByIdWithMemberForUpdate`(`@Lock(PESSIMISTIC_WRITE)` + 3000ms 타임아웃)를 호출해, 동시 구매 시도 시 두 번째 트랜잭션이 row 락에 걸려 대기하다 재조회 후 정상 거부된다. row-level 락으로 race를 원천 차단하는 구조.
- 트랜잭션 경계 패턴이 채팅 도메인과 일관적이다(클래스 레벨 readOnly + 쓰기 메서드만 override).
- 목록 조회 쿼리가 QueryDSL DTO 프로젝션으로 join해 N+1이 없고, `limit(size+1)` + `SliceImpl` 트릭도 다른 도메인과 일관적이다.
- 기본 무결성 체크(SELLING 상태 검증, 본인 상품 구매 방지)는 즉시구매 경로에서 정상 동작한다.
- `ORDER_LIFECYCLE_GUIDE.md`가 상태 머신 표, 코드 스니펫, 예외 3종, 스케줄러, 채팅 OFFER 연동, 구현 순서까지 실행 가능한 수준으로 구체적이다.

## 다음에 손대면 좋을 순서

1. **`Item.status` 뒷문 제거를 최우선으로 한다** — `ItemUpdateDto.status` 필드와 `Item.updateItem`의 상태 분기를 삭제. 이걸 먼저 안 하면 아래에서 아무리 정교한 상태 머신을 만들어도 우회로가 남는다.
2. **`Orders` 엔티티 확장** — `agreedPrice` 필드, `accept()`/`pay()`/`ship()`/`confirm()`/`cancel()` 전이 메서드 추가. `OrderStatus`에 `SHIPPING` 추가. 예외 3종 신설 후 매핑.
3. **`OrdersRepository`에 fetch-join 조회 메서드 추가** — `orderId` 하나로 `buyer`/`item`/`item.seller`까지 한 번에 가져오는 쿼리.
4. **`OrdersService.changeStatus` + `OrdersController` PATCH 구현** — `ORDER_LIFECYCLE_GUIDE.md` 3~4절 그대로.
5. **`ItemException`의 404 고정 매핑 문제 정리** — 즉시구매 에러(404 vs 409)를 구분할 수 있도록 예외 분리, 문서가 약속한 409와 실제 코드를 일치시킨다.
6. **락 타임아웃 예외 매핑 추가** — `PessimisticLockException`/`LockTimeoutException` → 409/503.
7. **목록 조회 필터 확장 + DTO 필드 추가** — `orderStatus.in(...)`을 CANCELED만 제외하는 형태로 넓히고 `orderStatus`/`agreedPrice` 노출.
8. **자동취소 스케줄러는 마지막** — 상태전이 자체가 동작해야 의미가 있으므로 가장 후순위.
</content>
