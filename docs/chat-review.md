# 채팅(WebSocket/STOMP) 백엔드 리뷰

`domain/chat/**`, `web/config/WebSocketConfig.java`, `web/exception/chat/**`를 대상으로 한 코드 리뷰 기록. 최신 코드 기준으로 정리했으며, 해결된 항목과 남은 항목을 분리했다. 엔드포인트 계약 자체는 [`docs/api/chat.md`](api/chat.md)가 기준 문서다.

## 해결됨

- **메시지 이력 조회** — 과거 `ChatRoomController`에 주석 처리되어 있었으나 현재 `GET /api/chat/rooms/{roomId}`로 완전히 구현·활성화되어 있다(`ChatRoomService.getMessages`).
- **채팅방 목록 조회 API** — `GET /api/chat/rooms/me`로 구현되어 있고, 프론트(`ChatList.jsx`)도 이 API로 연동되어 있다(과거의 localStorage 우회는 제거됨).
- **`ChatRoomException` HTTP 매핑 누락** — `GlobalExceptionHandler.chatRoomException(ChatRoomException e)`가 `e.getHttpStatus()`를 반환하도록 이미 수정되어, 401/403/404/409가 정상적으로 나간다.
- **STOMP 목적지 변수 이름 불일치** — 최초 리뷰에서 우려했던 것과 달리 `@DestinationVariable`과 파라미터명(`chatRoomId`)이 일치해 문제 없음.
- **N+1 쿼리 우려** — `ChatRoomResponse.from`이 `seller.getId()`처럼 식별자만 접근하므로 Hibernate 프록시 특성상 추가 쿼리가 발생하지 않는다. `seller`의 다른 필드(nickname 등)를 노출하게 되면 그때 fetch join을 추가해야 한다.
- **채팅 예외에 대한 STOMP 레벨 에러 처리가 없음** — `ChatMessageController`에 `@MessageExceptionHandler(ChatMessageException.class)`/`@MessageExceptionHandler(ChatRoomException.class)`/`@MessageExceptionHandler(MethodArgumentNotValidException.class)`를 추가해 예외 발생 시 서버 로그(slf4j `warn`)를 남기도록 했다. `/user/queue/errors` 같은 개인 큐로 `{error, message}`를 클라이언트에 돌려주는 것은 STOMP Principal(커스텀 `DefaultHandshakeHandler`)이 추가로 필요해 후속 과제로 남겨둔다.
- **WebSocket 핸드셰이크 단계에서 인증 검증이 없음** — `web/interceptor/LoginHandshakeInterceptor`를 추가해 `WebSocketConfig.registerStompEndpoints`에서 `HttpSessionHandshakeInterceptor`보다 먼저 실행되게 등록했다. HTTP 세션에 `SessionConst.LOGIN_MEMBER`가 없으면 핸드셰이크 자체를 401로 거부한다.
- **STOMP 메시지에 Bean Validation 미적용** — `ChatMessageController.sendMessage`에는 이미 `@Valid ChatMessageRequest`가 적용돼 있었다(과거 리뷰 시점 이후 수정됨). 다만 검증 실패 시 `MethodArgumentNotValidException`을 잡는 핸들러가 없어 조용히 연결만 끊기던 문제가 있었는데, 위 STOMP 예외 처리 항목에서 함께 로깅하도록 고쳤다.
- **가격 제안(OFFER) 기능 구현** — `ChatMessage`에 `messageType`(`MessageType.TEXT`/`OFFER`)과 `offeredPrice`, `offerStatus`(`OfferStatus`: `PENDING`/`ACCEPTED`/`REJECTED`, 아직 상태 전이 API는 없음) 필드가 추가되었고, `POST /api/chat/rooms/offer`(`ChatRoomController.createOffer` → `ChatRoomService.createOffer` → `ChatMessageService.sendOffer`)로 채팅방이 없으면 생성, 있으면 재사용해서 OFFER 메시지를 만들고 STOMP로 브로드캐스트한다.
- **`offerStatus`가 설정/노출되지 않던 문제** — 과거 리뷰에서 `ChatMessage.offerChatMessage()`가 `offerStatus`를 대입하지 않고 응답 DTO에도 노출되지 않는다고 지적했으나, 현재는 `offerChatMessage()`가 생성 시점에 `offerStatus = OfferStatus.PENDING`을 명시적으로 대입하고(`ChatMessage.java:70`), `ChatMessageResponse`에도 `messageType`/`offerStatus` 필드가 노출되어 있어 해결된 상태다.
- **`ChatMessageException`이 REST 경로에서 처리되지 않던 문제** — `GlobalExceptionHandler`에는 `ChatRoomException`만 매핑돼 있고 `ChatMessageException`은 STOMP 전용 `@MessageExceptionHandler`에서만 잡혔다. `POST /api/chat/rooms/offer`는 REST 엔드포인트라 이 핸들러가 적용되지 않아, 빈 내용/저가 제안 시 500 whitelabel 에러가 나가던 문제가 있었다. `GlobalExceptionHandler`에 `ChatMessageException`(400)과 REST용 `MethodArgumentNotValidException`(400) 핸들러를 추가해 해결했다.
- **가격 제안 요청에 Bean Validation 미적용** — `ChatRoomRequest`에 검증 애너테이션이 없고 컨트롤러에도 `@Valid`가 없어 `content: null`, `offeredPrice: null`에서 NPE가 나던 문제가 있었다. `content`에 `@NotBlank`, `offeredPrice`에 `@NotNull @Min(1000)`을 추가하고 `createOffer`에 `@Valid`를 적용해 해결했다(서비스 레이어의 수동 가격 체크는 중복이라 제거).
- **`createOffer`가 응답 바디 없이 `void`를 반환하던 문제** — STOMP 브로드캐스트에만 의존해서, 신규 채팅방이 생성되는 첫 제안 케이스에서는 프론트가 `roomId`를 미리 알 수 없어 토픽 구독 전에 브로드캐스트가 나가버리는 레이스가 있었다. `createOffer`가 `ResponseEntity<OfferedMessageResponse>`를 반환하도록 고쳐 HTTP 응답으로도 방 정보를 받을 수 있게 했다.
- **`ChatMessageService.sendOffer`의 `@Transactional` 누락** — `sendMessage`는 명시적으로 `@Transactional`이 붙어 있는데 `sendOffer`는 클래스 레벨 기본값(`readOnly = true`)을 그대로 상속하고 있었다. 현재 호출 경로(`ChatRoomService.createOffer`가 이미 쓰기 트랜잭션을 열어둠)에서는 드러나지 않지만 `sendOffer`가 다른 경로로 단독 호출되면 `chatRoom.updateLastMessageAt()` 같은 더티체킹 갱신이 flush 안 될 위험이 있어 `sendMessage`와 동일하게 명시적 `@Transactional`을 추가했다.
- **가격 제안 수락(accept) API 구현** — `POST /api/chat/rooms/{roomId}/offer/{messageId}/accept`(`ChatRoomService.acceptOffer`)로 구현되었다. reject와 달리 `messageId`가 해당 `roomId` 소속인지·`messageType == OFFER`인지·`offerStatus == PENDING`인지·`item.status == SELLING`인지를 모두 검증하고, 수락 시 `ItemService.reserveForOffer`(`RESERVED` 전이)와 `OrdersService.createOrders`(주문 생성, `OrderStatus.ACCEPTED`)를 함께 호출하며, 같은 상품의 다른 `PENDING` 제안들은 `ChatMessageRepository.rejectOtherOffers`(native UPDATE)로 일괄 거절 처리한다. 초기 구현 시 메서드에 `@Transactional`이 빠져 있어(클래스 기본값 `readOnly=true`로 실행) 쓰기 작업이 read-only 트랜잭션에서 돌 뻔한 문제와, 권한 실패 메시지가 reject 문구("제안을 거절할 권한이 없습니다.")를 그대로 복붙해 쓰던 오탈자를 리뷰 과정에서 발견해 함께 수정했다. 상세 계약은 [`docs/api/chat.md`](api/chat.md)의 accept 섹션 참고.
- **응답에 `orderId`가 없던 문제** — `OrdersService.createOrders`가 `void`를 반환해 accept 시 생성된 주문 id를 프론트에 내려줄 방법이 없었다. `createOrders`가 생성된 주문의 id(`Long`)를 반환하도록 고치고, `ChatMessageResponse`에 nullable `orderId` 필드를 추가해(`from(message)`는 `null`, accept 전용 `from(message, orderId)` 오버로드 추가) accept 응답에서만 값이 채워지도록 했다.
- **`Item.price`를 협상가로 영구 덮어쓰던 설계** — 초기 구현은 `Item.acceptOfferPrice`로 상품의 원래 `price`를 제안 가격으로 완전히 대체해 원가 이력이 사라지는 문제가 있었다. `Orders`에 `agreedPrice`(Integer) 필드를 추가해 "이 주문이 성사된 실제 가격"을 여기에 저장하도록 바꾸고, `Item.price`는 건드리지 않는 방식으로 리팩터링했다(`Item.acceptOfferPrice` 메서드 자체를 삭제, `ItemService.reserveForOffer`는 상태 전이만 수행). 즉시구매(`OrdersService.save`)도 `agreedPrice = item.getPrice()`로 채워 두 경로가 동일한 필드를 공유한다. 프론트(`normalize.js`/`SalesHistory.jsx`/`PurchaseHistory.jsx`)는 이미 `agreedPrice`를 소비하도록 구현되어 있었어서 백엔드만 채워주면 되는 상태였다.

## 남은 이슈

### 1. 판매자는 `/offer` 엔드포인트로 역제안을 보낼 수 없음

`ChatRoomController.createOffer` → `ChatRoomService.createOffer`는 항상 `createChatRoom(itemId, memberId)`를 먼저 호출하는데, `createChatRoom`은 `item.getSeller().getId().equals(buyerId)`이면 "본인이 등록한 상품에는 문의할 수 없습니다"로 무조건 거부한다. 즉 이미 열린 채팅방에서 판매자가 가격을 역제안하려고 이 엔드포인트를 호출해도 항상 409로 막힌다. 구매자만 제안 가능하도록 의도한 MVP 범위 제한인지, 놓친 케이스인지 확인이 필요하다 — 판매자 역제안을 지원하려면 판매자 호출 시엔 `createChatRoom`을 거치지 않고 `roomId` 기준으로 기존 방을 바로 찾아 `sendOffer`만 호출하도록 분기해야 한다.

### 2. 동시 중복 제출 시 유니크 제약 위반이 매핑되지 않음

`ChatRoom`에 `(item_id, buyer_id)` 유니크 제약이 있어 최악의 경우(더블클릭 등 동시 요청)에도 DB 레벨에서 막히지만, `DataIntegrityViolationException`을 처리하는 핸들러가 없어 이 경우 500으로 노출된다. 레이스 자체가 드물어 우선순위는 낮다.

### 3. `createOffer` 처리 중 채팅방 조회가 중복됨

`ChatRoomService.createOffer`가 `createChatRoom`(조회/생성)으로 `ChatRoomResponse` DTO만 얻고, `ChatMessageService.sendOffer`가 `roomId`로 `ChatRoom` 엔티티를 다시 fetch join 조회한다. 기능상 문제는 없지만 쿼리가 한 번 더 나간다 — 필요하면 엔티티를 그대로 넘기는 오버로드로 정리할 수 있다(우선순위 낮음).

### 4. `rejectOffer`는 여전히 방/타입/상태 검증이 생략되어 있음

거절(reject, `POST /api/chat/rooms/{roomId}/offer/{messageId}/reject`)은 `messageId`로 `ChatMessage`를 전역 조회할 뿐, accept에서 새로 추가한 것과 같은 "해당 `roomId` 소속인지 / `messageType == OFFER`인지 / `offerStatus == PENDING`인지" 검증이 없다. reject는 side effect가 메시지 상태 변경뿐이라 상대적으로 영향이 적어 이번 accept 작업 범위에서는 그대로 뒀지만, 동일한 검증을 reject에도 넣는 편이 일관적이다. 권한 오류도 `ChatRoomException`이 아닌 `UnauthorizedException`(401)을 쓰는 점은 accept와 동일하게 유지됨(의도적 설계인지 확인 필요).

### 5. `Orders.agreedPrice`가 있어도 기본 목록 조회에서는 `ACCEPTED` 주문이 보이지 않음

오퍼 수락으로 생성되는 주문은 `OrderStatus.ACCEPTED`인데, `OrdersRepositoryImpl.findAllPurchases`는 `orderStatus.in(COMPLETED, PAY_COMPLETED)`로 고정되어 있고 `findAllSales`도 `status` 파라미터가 없으면 동일한 필터를 쓴다(`docs/orders-review.md` 7번 참고). 즉 구매자의 "구매 내역"에는 수락된 제안 주문이 전혀 안 뜨고, 판매자도 `GET /api/orders/sales?status=ACCEPTED`로 명시적으로 필터링해야만 보인다 — `agreedPrice` 자체는 정상 저장되지만 노출 경로가 좁다.

## 잘 되어 있는 부분

- STOMP 브로커 설정(`enableSimpleBroker("/topic","/queue")` + `setApplicationDestinationPrefixes("/app")`)과 발행/구독 목적지 체계가 일관적이다.
- `ChatRoom`(`item_id, buyer_id` unique 제약)과 `ChatMessage`(FK, `nullable=false`) 엔티티 설계가 견고하다 — 같은 상품에 같은 구매자가 중복으로 방을 만드는 것을 DB 레벨에서 막는다.
- `ChatRoomService.createChatRoom`이 판매자 본인이 자기 상품에 채팅을 걸지 못하도록 막는 검증과, 이미 연 방이 있으면 기존 방을 반환하는 멱등 처리가 잘 되어 있다.
- `ChatMessageService.sendMessage`의 인가 체크(`containsMember`)가 정상 동작한다 — 방에 속하지 않은 제3자는 메시지를 보낼 수 없다.

## 다음에 손대면 좋을 순서

1. `rejectOffer`에도 accept와 동일한 방/타입/상태 검증 추가(위 4번), 판매자 역제안 지원 여부 결정 — 역제안을 지원한다면 `/offer` 호출 시 판매자/구매자 분기 처리도 함께.
2. `findAllPurchases`/`findAllSales`의 상태 필터를 넓혀 `ACCEPTED` 주문도 기본 목록에 노출(위 5번, `docs/orders-review.md` 7번과 동일 작업).
3. STOMP 예외를 `/user/queue/errors` 개인 큐로 클라이언트에 돌려주기 — Principal 연동(커스텀 `DefaultHandshakeHandler`)이 선행되어야 한다.
4. `DataIntegrityViolationException` → 409 매핑, `createOffer`의 채팅방 중복 조회 정리(둘 다 우선순위 낮음).
