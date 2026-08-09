# Orders(구매/판매/배송) 백엔드 리뷰

`domain/orders/**`, `domain/ordershistory/**`, `domain/item/Item.java`(상태 전이 관련), `web/exception/GlobalExceptionHandler.java`를 대상으로 한 코드 리뷰 기록. 기준 문서는 [`docs/api/orders.md`](api/orders.md)다. 즉시구매, 채팅 가격제안 연동, 상태전이(PATCH), 운송장 등록까지 이미 구현되어 있고, 아래는 그 위에서 남아 있는 이슈다.

## 해결됨 (과거에 미구현/이슈였던 항목)

- **주문 상태전이 로직 구현 완료** — `OrderStatus`에 `REJECTED`/`SHIPPING`이 추가되어 총 7종(`REQUESTED, REJECTED, ACCEPTED, CANCELED, PAY_COMPLETED, SHIPPING, COMPLETED`)이 되었고, `PATCH /api/orders/{orderId}`(`OrdersService.changeStatus`)가 ACCEPT/PAY/SHIP/CONFIRM/CANCEL 액션과 권한·상태 검증(403/409)을 모두 구현했다.
- **협상가(`agreedPrice`) 저장** — `Orders.agreedPrice`가 즉시구매(`item.getPrice()`)와 채팅 제안(`ChatMessage.offeredPrice`) 양쪽에서 채워진다.
- **운송장(배송) 관리** — `Orders.trackingCompany`/`trackingNumber` 필드와 `PATCH /api/orders/{orderId}/tracking`이 구현되어, 판매자가 발송 후 운송장 정보를 등록할 수 있다.
- **상태변경 감사로그** — `domain/ordershistory`(`OrdersHistory` + `OrdersHistoryService.save`)가 상태가 바뀔 때마다 스냅샷을 append한다.
- **목록 조회 기본 필터 수정 완료** — `PurchaseHistory.jsx`가 탭마다 `TAB_STATUSES`(`REQUESTED`/`ACCEPTED`/`PAY_COMPLETED,SHIPPING,COMPLETED`)로 `status`를 항상 명시적으로 전달하도록 바뀌어, "구매완료" 탭에 취소·거절된 주문까지 섞여 보이던 문제가 해결됨.
- **`ItemException`이 `ErrorCode`를 갖도록 리팩터링 완료** — `OrdersException`/`ChatRoomException`과 동일하게 공통 부모 `ApplicationException`이 생성자에서 `ErrorCode`를 받고, `GlobalExceptionHandler.applicationException`이 `e.getErrorCode().getStatus()`로 응답하도록 바뀌었으며, `OrdersService`/`ItemService.update` 등 호출부도 404/409/403을 상황에 맞게 던지도록 갱신됨.
- **락 타임아웃 예외 매핑 추가 완료** — `GlobalExceptionHandler`에 `PessimisticLockingFailureException` 핸들러가 추가되어 409로 응답한다. Hibernate가 던지는 `PessimisticLockException`/`LockTimeoutException`은 Spring이 이 예외로 변환해 전달하므로 함께 커버됨.
- **에러 응답이 `{error, message}` JSON 포맷으로 전환 완료** — `GlobalExceptionHandler`가 `ErrorResponse(error, message)`를 반환하도록 전 도메인이 함께 리팩터링됐다.

## 남은 이슈

### 1. `Item.updateItem`의 `status` 뒷문이 여전히 열려 있다

`Item.java:102-103` — `ItemUpdateDto.getStatus()`가 null이 아니면 그대로 반영된다. 판매자가 상품 수정 API로 `Orders`/주문 상태전이와 무관하게 `Item.status`를 임의로 바꿀 수 있어, 정교한 주문 상태 머신을 만들어도 이 경로로 이중판매·상태 불일치가 여전히 가능하다([`docs/items-review.md`](items-review.md)와 동일 이슈).

### 2. 운송장 등록에 주문 상태 가드가 없다

`OrdersService.registerTracking`은 판매자 본인 여부만 검증하고 `orders.getOrderStatus()`가 어떤 값이어도 통과시킨다. `REQUESTED`/`CANCELED` 단계에서도 운송장을 등록할 수 있어, 정상 흐름(`PAY_COMPLETED` 이후에만 등록)을 강제하려면 상태 검증을 추가해야 한다.

### 3. 채팅 가격제안 "거절" 시 대응하는 `REQUESTED` 주문이 취소되지 않는다

`ChatRoomService.rejectOffer`는 `ChatMessage.offerStatus`만 `REJECTED`로 바꿀 뿐, `OrdersService.createNegotiationOrder`가 만들어 둔 `REQUESTED` `Orders` 레코드는 그대로 남는다(취소 처리 없음). 목록 조회 기본 필터는 수정됐지만(위 "해결됨" 참고), 이 orphan 주문은 여전히 `REQUESTED` 상태이므로 `PurchaseHistory.jsx`의 "제안 중인 내역" 탭(`status=REQUESTED`)에는 거절된 제안이 계속 노출된다. `rejectOffer`에서도 `orders.updateOrderStatus(REJECTED)` 같은 처리를 함께 호출해야 한다([`docs/chat-review.md`](chat-review.md)와 교차 참고).

### 4. 자동취소 스케줄러가 로그만 남기고 실제로 아무것도 하지 않는다

`OrdersAutoCancelScheduler`는 매시 정각 실행되지만 `PAY_COMPLETED` 상태로 오래 방치된 주문을 실제로 `CANCELED`/`Item.SELLING`으로 되돌리는 로직이 없다(구현 자체가 비어 있음).

### 5. SweetTracker 등 외부 택배사 실시간 배송조회는 여전히 미구현

`frontend/src/api/delivery.js`의 `trackShipment`는 SweetTracker API 연동 골격만 있고 프로젝트 어디에서도 호출되지 않는 죽은 코드다(`.env`에 `VITE_SWEETTRACKER_KEY`도 설정된 적 없음). 현재 구현된 "배송 관리"는 판매자가 택배사명/운송장번호 문자열을 직접 입력해 저장하는 수준(위 "해결됨" 항목)이며, 실시간 배송 상태 조회는 별개의 선택적 확장 과제로 남아 있다. 필요 없다고 판단되면 `trackShipment`/`CARRIERS` 관련 죽은 코드를 함께 정리하는 것도 방법이다.

## 잘 되어 있는 부분

- 즉시구매 동시성은 실제로 안전하다 — `findByIdWithMemberForUpdate`(비관적 락)로 동시 구매 시도 시 두 번째 트랜잭션이 정상적으로 거부된다.
- 상태전이(`changeStatus`) 자체는 `requireStatus`/`requireRole`로 역행·권한 위반을 꼼꼼히 막고 있고, 채팅 수락 경로(`acceptNegotiation`)와 `applyAccept` 로직을 공유해 중복이 없다.
- 상태 변경마다 `ordershistory`에 감사로그가 남아 추적이 가능하다.
- 목록 조회 쿼리가 QueryDSL DTO 프로젝션으로 N+1 없이 조회되고, `limit(size+1)` + `SliceImpl` 트릭도 다른 도메인과 일관적이다.

## 다음에 손대면 좋을 순서

1. **채팅 거절 시 `REQUESTED` 주문도 함께 취소** — `rejectOffer`에 `orders.updateOrderStatus(REJECTED)` 호출 추가(위 3번, `chat-review.md`와 동일 작업).
2. **`Item.status` 뒷문 제거** — `ItemUpdateDto.status` 필드와 `Item.updateItem`의 상태 분기 삭제.
3. **운송장 등록에 상태 가드 추가** — `PAY_COMPLETED` 이후에만 등록 허용.
4. **자동취소 스케줄러 실제 구현** — 나머지가 안정된 뒤 가장 후순위.
5. (우선순위 낮음) SweetTracker 연동 여부 결정 — 필요 없으면 죽은 코드(`trackShipment`) 정리, 필요하면 API 키 발급/백엔드 프록시 설계.
