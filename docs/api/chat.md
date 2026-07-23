# Chat API — 채팅

소스: `domain/chat/chatroom/**`, `domain/chat/chatmessage/**`, `web/config/WebSocketConfig.java`. REST(채팅방 생성/목록/이력/가격제안)와 STOMP WebSocket(실시간 일반 메시지)이 함께 쓰인다.

코드리뷰(개선 방향)는 [`docs/chat-review.md`](../chat-review.md) 참고.

- REST 베이스 경로: `/api/chat/rooms`. STOMP 핸드셰이크: `/ws-chat`, 발행 prefix `/app`, 브로커 `/topic`.
- `ChatRoom`: `item`/`buyer`로 파생, `(item_id, buyer_id)` unique 제약으로 중복 방 생성 방지.
- `ChatMessage`: `messageType`(`TEXT`/`OFFER`), `content`, `offeredPrice`(OFFER일 때만) 보유.

---

### POST /api/chat/rooms (채팅방 생성/조회), GET /api/chat/rooms/me (목록), GET /api/chat/rooms/{roomId} (이력)

**상태**: 구현됨. 인증 필요, buyer/seller만 조회 가능. 프론트(`ChatList.jsx`/`ChatRoom.jsx`)와 실제로 연동되어 있다(로컬 스토리지 사용 안 함).

### STOMP 실시간 메시지 (일반 문의)

**상태**: 구현됨. `/app/chat/rooms/{chatRoomId}/messages` 발행 → `/topic/chat/rooms/{chatRoomId}` 구독. 핸드셰이크 단계에서 세션 인증 검증(`LoginHandshakeInterceptor`)까지 되어 있다.

### POST /api/chat/rooms/offer (가격 제안 생성)

**상태**: 구현됨(REST 엔드포인트, STOMP 아님). 채팅방이 없으면 생성 후 OFFER 메시지를 저장하고 `/topic/chat/rooms/{roomId}`로도 브로드캐스트한다. `ChatMessage.offerStatus`는 생성 시점에 `PENDING`으로 대입되고, 응답 DTO(`ChatMessageResponse`)에도 `messageType`/`offerStatus`가 노출된다.

**거절**과 **수락** 모두 구현되어 있다. 상세는 아래 섹션 참고.

### POST /api/chat/rooms/{roomId}/offer/{messageId}/reject (가격 제안 거절)

**상태**: 구현됨. 요청 바디 없음(`@RequestBody` 미사용) — `roomId`/`messageId` 경로변수와 `@Login LoginMember` 인증만 사용한다.

- **권한**: `chatRoom.getItem().getSeller()`가 로그인 사용자와 일치해야 한다(판매자만 거절 가능). 불일치 시 `UnauthorizedException`(401)을 던진다 — 의미상 403(`ChatRoomException(HttpStatus.FORBIDDEN, ...)`)이 더 적합하지만 현재 구현은 401로 응답한다는 점에 유의.
- **검증**: `messageId`로 `ChatMessage` 조회 실패 시에만 `ChatMessageException`(400)을 던진다. **`messageId`가 실제로 해당 `roomId` 소속인지, `messageType`이 `OFFER`인지, `offerStatus`가 `PENDING`인지는 검증하지 않는다** — 알려진 제약이며 후속 개선 과제로 남아 있다(`docs/chat-review.md` 참고).
- **처리**: 원본 `ChatMessage`의 `offerStatus`만 바꾸는 것이 아니라, `"거절합니다."` 내용의 **새 `ChatMessage` 레코드**를 추가로 저장한다(`ChatMessage.rejectOfferMessage(...)`). 원본 메시지도 `changeStatusToReject()`로 `offerStatus = REJECTED`가 되지만, 응답/브로드캐스트에 담기는 메시지는 이 새 레코드다.
- **응답**: `ChatRoomAndMessageDto`(`{room, message}`), HTTP **201 CREATED**.
- **실시간 동기화**: 처리 후 컨트롤러에서 `messagingTemplate.convertAndSend("/topic/chat/rooms/"+roomId, response)`로 브로드캐스트(`createOffer`와 동일 패턴).
- **프론트 연동**: `frontend/src/api/client.js`의 `rejectOffer(roomId, messageId)`(`POST`, 바디 없음)를 통해 `frontend/src/pages/ChatRoom.jsx`의 `handleRejectOffer`가 호출한다. 과거 STOMP `publish("/app/chat/rooms/{roomId}/offers/{messageId}/reject")` 방식(`docs/ORDER_LIFECYCLE_GUIDE.md` 8절 설계)은 폐기되었다.

### POST /api/chat/rooms/{roomId}/offer/{messageId}/accept (가격 제안 수락)

**상태**: 구현됨. `frontend/src/api/client.js`의 `acceptOffer(roomId, messageId)` → `frontend/src/pages/ChatRoom.jsx`의 `handleAcceptOffer`와 연동된다.

- **요청**: `@RequestBody` 없음. `POST /api/chat/rooms/{roomId}/offer/{messageId}/accept`, 경로변수 `roomId`/`messageId` + `@Login LoginMember` 인증만 사용한다.
- **권한**: reject와 동일하게 `chatRoom.getItem().getSeller()`가 로그인 사용자와 일치해야 한다(판매자만 수락 가능). 불일치 시 `UnauthorizedException`(401).
- **검증**: reject와 달리 다음을 모두 검증한다 — `messageId`가 해당 `roomId`의 메시지인지, `messageType == OFFER`인지(둘 중 하나라도 아니면 `ChatMessageException` 400), `offerStatus == PENDING`인지(이미 처리된 제안이면 `ChatMessageException` 400), `item.status == SELLING`인지(이미 예약/판매완료된 상품이면 `ItemException` 400).
- **처리**:
  1. `Item.status`를 `RESERVED`로 전이시킨다(`ItemService.reserveForOffer`). **`Item.price`는 건드리지 않는다** — 상품 정가는 그대로 보존된다.
  2. `OrdersService.createOrders(buyer, item, OrderStatus.ACCEPTED, offeredPrice)`로 주문을 생성하면서, 제안 가격을 `Orders.agreedPrice`(협상가 전용 필드)에 저장한다.
  3. 원본 `ChatMessage`를 `changeStatusToAccept()`로 `ACCEPTED` 처리하고, `"제안을 수락합니다."` 내용의 새 `ChatMessage` 레코드를 추가로 저장한다(reject와 동일한 "새 레코드 추가" 패턴).
  4. 같은 상품(`item_id`)의 다른 채팅방에 걸려 있는 `PENDING` 상태 OFFER 메시지를 전부 `REJECTED`로 일괄 처리한다(`ChatMessageRepository.rejectOtherOffers`, native UPDATE).
- **응답**: `ChatRoomAndMessageDto`(`{room, message}`), HTTP **201 CREATED**. `message.orderId`에 위 2번에서 생성된 주문의 id가 실린다(reject/일반 메시지 응답에서는 `orderId`가 `null`).
- **실시간 동기화**: 처리 후 `/topic/chat/rooms/{roomId}`로 브로드캐스트(reject/createOffer와 동일 패턴).

⚠️ **알려진 제약**: `Orders`에 `agreedPrice`가 저장되지만, `GET /api/orders/purchases`/`GET /api/orders/sales`(파라미터 없이 호출 시)는 `orderStatus.in(COMPLETED, PAY_COMPLETED)`로 필터링되어 있어 `ACCEPTED` 상태인 오퍼 수락 주문은 기본 목록에 나타나지 않는다 — 판매자가 `GET /api/orders/sales?status=ACCEPTED`로 명시적으로 필터링해야 보인다(상세는 [`docs/orders-review.md`](../orders-review.md) 참고).
</content>
