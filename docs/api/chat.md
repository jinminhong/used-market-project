# Chat API — 채팅

⚠️ **정정 (2026-07-22, 코드 직접 대조로 재검증)**: 채팅은 `domain/chat/chatmessage`, `domain/chat/chatroom` 패키지와 `WebSocketConfig`로 이미 구현되어 있습니다. 다만 이전 버전의 이 정정 메모에도 부정확한 부분이 있어 아래처럼 다시 정리합니다:

- REST 채팅방 생성: **`POST /api/chatRooms`**(camelCase, 슬래시 없음, body `{itemId}`, 멱등 생성/조회) — 이 문서가 원래 제안한 `POST /api/items/{itemId}/chatrooms`도 아니고, 이전 정정 메모가 적었던 `POST /api/chat/rooms`(슬래시 있음)도 아닙니다. **`frontend/src/api/client.js`의 `createChatRoom`은 여전히 `/api/chat/rooms`를 호출하고 있어 실제 백엔드 경로와 다릅니다 — `Detail.jsx`의 "구매 문의 · 가격제안" 버튼은 현재 404가 납니다.** 아래 "⚠️ 알려진 버그" 참고.
- 메시지 이력 REST 조회: 이전 정정 메모는 "컨트롤러에 주석 처리된 채 미완성"이라고 적었으나 **틀린 서술이었습니다.** 실제로는 `ChatRoomController.getMessages()`가 **`GET /api/chatRooms/{roomId}`**(경로에 `/messages` 접미사 없음)로 완전히 구현·활성화되어 있습니다.
- 내 채팅방 목록: `GET /api/chatRooms/me`로 백엔드는 구현되어 있으나, **프론트(`ChatList.jsx`)는 이 API를 전혀 호출하지 않고** `chatStorage.js`의 로컬 스토리지만 사용합니다 — 목록 화면은 백엔드와 연동되어 있지 않습니다.
- STOMP 핸드셰이크: `/ws-chat`. 발행 prefix: `/app`(제안한 `/pub`가 아님). 브로커: `/topic`, `/queue`(제안한 `/sub`가 아님). 이 부분은 실제 코드와 일치합니다.
- 발행 destination: `/app/chat/rooms/{chatRoomId}/messages`. 구독: `/topic/chat/rooms/{chatRoomId}`. 이 부분도 실제 코드와 일치합니다.

가격 제안(OFFER) 기능은 여전히 미구현이며(엔티티/서비스 전체 grep 0건으로 재확인), 아래 "OFFER 메시지" 절이 실제 경로 기준의 확정 설계입니다. 나머지 원문(엔티티 제안, REST 제안 경로)은 최초 설계 의도를 남겨두기 위해 그대로 두되, 위 정정 사항을 우선합니다.

## ⚠️ 알려진 버그 (2026-07-22 코드 리딩으로 발견, 수정 안 됨)

1. **프론트/백엔드 경로 불일치** — `frontend/src/api/client.js`의 `createChatRoom`이 `POST /chat/rooms`(→ 실제 요청 URL `/api/chat/rooms`)를 호출하지만 백엔드는 `/api/chatRooms`에만 매핑되어 있습니다. 상품 상세 페이지의 "구매 문의 · 가격제안" 버튼(`Detail.jsx`)을 누르면 실제로 404가 발생합니다.
2. **응답 직렬화 위험** — `PageMessageResponseDto`, `PageChatRoomResponse`, `ChatMessageDto` 세 클래스 모두 Lombok이나 getter 없이 `private`/package-private 필드만 갖고 있습니다. Spring Boot 기본 Jackson 설정은 public getter나 public 필드만 직렬화하므로, `GET /api/chatRooms/me`와 `GET /api/chatRooms/{roomId}`의 응답 바디가 실제로는 `{}`(빈 객체)나 빈 리스트 항목으로 나갈 가능성이 높습니다. (런타임 확인 전이므로 "위험"으로 표시 — 확인되면 각 DTO에 `@Getter` 추가 필요.)
3. **예외 처리 버그** — `GlobalExceptionHandler.chatRoomException(HttpStatus httpStatus, ChatRoomException e)`가 `e.getHttpStatus()`를 꺼내지 않고 `HttpStatus`를 별도 메서드 파라미터로 선언하고 있습니다. Spring MVC `@ExceptionHandler` 메서드는 `HttpStatus` 타입을 자동으로 바인딩할 표준 리졸버가 없어, `ChatRoomException`이 발생하는 모든 경로(채팅방 없음/권한 없음/본인 상품에 채팅 시도 등)에서 의도한 상태 코드 대신 500이 날 가능성이 높습니다. 올바른 구현은 `ResponseEntity.status(e.getHttpStatus()).body(e.getMessage())`.
4. **핸들러 누락** — `ChatMessageException`에 대응하는 `@ExceptionHandler`가 `GlobalExceptionHandler`에 없습니다. 빈 메시지 전송 시도, 세션 없는 STOMP 발행 등에서 처리되지 않은 예외로 500이 됩니다.
5. `ChatRoomResponse`의 필드명 오타: `sellerNickName`이 아니라 `sellerNicName`("k" 없음)입니다. 프론트가 이 필드를 참조한다면 확인이 필요합니다.

## 신규 엔티티 (실제 구현, 원문 제안과 다름)

- `ChatRoom`: `id`, `item`(`@ManyToOne`), `buyer`(`@ManyToOne Member`), `createdAt`, `lastMessageAt`. **`seller` 컬럼은 없습니다** — `item.getSeller()`로 파생해서 사용합니다(원문 제안은 별도 `seller` 필드가 있다고 가정했으나 실제로는 없음). `(item_id, buyer_id)` 조합 unique(`uk_chat_room_item_buyer`) — 같은 상품에 같은 구매자가 채팅방을 중복 생성하지 않도록.
- `ChatMessage`: `id`, `chatRoom`(`@ManyToOne`), `sender`(`@ManyToOne Member`), `content`(최대 1000자), `sentAt`. `type`/`offeredPrice`/`offerStatus`/`orderId` 등 OFFER 관련 필드는 아직 없습니다.

---

### POST /api/chatRooms (채팅방 생성/조회)

**상태**: 구현됨 (`ChatRoomController.createChatRoom` / `ChatRoomService.createChatRoom`)

**요청**
- 인증 필요(`@Login LoginMember`, 세션 없으면 `LoginMemberArgumentResolver`가 `null`을 주입하고 이후 NPE 가능성 — 인터셉터가 먼저 401을 던지므로 실무상 도달하지 않음). Body: `{ "itemId": 10 }`.
- 이미 해당 상품에 대해 내가 연 채팅방이 있으면 새로 만들지 않고 `findChatRoomByItemAndBuyer`로 기존 방을 그대로 반환(멱등).
- 판매자 본인이 자기 상품에 채팅을 시도하면 `ChatRoomException(HttpStatus.CONFLICT, ...)`.

**성공 응답** (`201 Created`, `ChatRoomResponse`)
```json
{
  "roomId": 1,
  "itemId": 10,
  "buyerId": 5,
  "sellerId": 3,
  "itemName": "item1",
  "sellerNicName": "seller1",
  "buyerNickName": "me"
}
```
`sellerNicName`은 오타가 아니라 실제 필드명입니다(위 "알려진 버그" 5번 참고).

**에러 응답** (실제로는 위 "알려진 버그" 3번으로 인해 아래 상태 코드가 그대로 나가지 않고 500이 될 가능성이 있습니다)

| 의도된 HTTP 상태 | 발생 조건 | 실제 예외 |
|---|---|---|
| 401 | 세션 없음 | `UnauthorizedException`(인터셉터 단계, 정상 동작) |
| 404 | 존재하지 않는 `itemId` | `ItemException("상품을 찾을 수 없습니다.")` — 이 핸들러는 정상 동작 |
| 409(의도) | 판매자 본인이 자기 상품에 채팅 시도 | `ChatRoomException(HttpStatus.CONFLICT, "본인이 등록한 상품에는 문의할 수 없습니다.")` — 핸들러 시그니처 버그로 500 가능 |

**프론트 연동 상태**: `frontend/src/api/client.js`의 `createChatRoom`이 실제로는 `/api/chat/rooms`를 호출해 이 엔드포인트와 경로가 다릅니다(위 버그 1번). 수정 없이는 프론트에서 채팅방 생성이 동작하지 않습니다.

---

### GET /api/chatRooms/me (내 채팅방 목록)

**상태**: 백엔드는 구현됨(`ChatRoomController.getChatRooms` / `ChatRoomService.getRooms`), **프론트는 미연동**

**요청**
- 인증 필요. `Pageable` 바인딩(쿼리 파라미터 `page`/`size`/`sort`).

**성공 응답** (`PageChatRoomResponse`, 원문 제안의 `{list, hasNext}`가 아니라 `{chatRooms, hasNext}`)
```json
{
  "chatRooms": [
    {
      "roomId": 1,
      "itemId": 10,
      "itemName": "item1",
      "storedFilename": "stored-item1-image1.jpg",
      "counterPartNickName": "seller1",
      "lastMessage": "네고 가능할까요?",
      "buyerId": 5,
      "sellerId": 3,
      "buyerNickName": "me",
      "sellerNickName": "seller1",
      "lastMessageAt": "2026-07-19T10:00:00"
    }
  ],
  "hasNext": false
}
```
`counterPartNickName`은 서비스에서 요청자가 buyer인지 seller인지에 따라 상대방 닉네임으로 채워집니다. `ChatRoomDto`는 `@Getter`가 있어 정상 직렬화되지만, 이를 감싸는 `PageChatRoomResponse` 자체에 getter가 없어(위 버그 2번) 실제 응답이 `{}`로 나갈 위험이 있습니다.

**프론트 연동 상태**: `frontend/src/pages/ChatList.jsx`는 이 API를 호출하지 않고 `getChatRooms()`(`frontend/src/lib/chatStorage.js`, 로컬 스토리지)만 사용합니다. 즉 "내 채팅" 목록 화면은 새로고침하거나 다른 기기로 접속하면 비어 보입니다.

**에러 응답**

| HTTP 상태 | 발생 조건 |
|---|---|
| 401 | 세션 없음(인터셉터 단계) |

---

### GET /api/chatRooms/{roomId} (메시지 이력)

**상태**: 구현됨(`ChatRoomController.getMessages` / `ChatRoomService.getMessages`). 경로에 `/messages` 접미사가 없다는 점에 주의(원문 제안은 `/api/chatrooms/{roomId}/messages`였음).

**요청**
- 인증 필요(해당 방의 buyer/seller만, `ChatRoom.containsMember`로 검사). Path: `roomId`. `Pageable` 바인딩.

**성공 응답** (`PageMessageResponseDto`, `ChatMessageDto` 기반)
```json
{
  "list": [
    {
      "roomId": 1,
      "itemId": 10,
      "senderId": 5,
      "sendAt": "2026-07-19T10:00:00",
      "senderNickname": "me",
      "content": "네고 가능할까요?"
    }
  ],
  "hasNext": false
}
```
필드명이 `sentAt`이 아니라 `sendAt`(QueryDSL 프로젝션 생성자 그대로)입니다. `PageMessageResponseDto`와 `ChatMessageDto` 둘 다 getter가 없어(위 버그 2번) 실제 응답이 `{}`나 빈 객체 배열로 나갈 위험이 있습니다.

**에러 응답** (역시 버그 3번으로 인해 500이 될 가능성 있음)

| 의도된 HTTP 상태 | 발생 조건 | 실제 예외 |
|---|---|---|
| 401 | 세션 없음 | `UnauthorizedException`(정상 동작) |
| 403(의도) | 방의 buyer/seller가 아닌 제3자가 조회 시도 | `ChatRoomException(HttpStatus.FORBIDDEN, "해당 채팅방에 참여할 수 없습니다.")` |
| 404(의도) | 존재하지 않는 `roomId` | `ChatRoomException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.")` |

---

### STOMP 실시간 메시지 (구현됨)

REST가 아니라 WebSocket/STOMP이므로 별도로 정리합니다. `WebSocketConfig`, `ChatMessageController`(`@MessageMapping`), `ChatMessageService` 확인 완료.

- 핸드셰이크: `/ws-chat`(`WebSocketConfig.registerStompEndpoints`), `HttpSessionHandshakeInterceptor`로 HTTP 세션 속성을 WebSocket 세션에 그대로 복사합니다. `allowedOriginPatterns`는 `http://localhost:5173`로 고정.
- 발행(클라이언트 → 서버): `/app/chat/rooms/{chatRoomId}/messages`
  ```json
  { "content": "네고 가능할까요?" }
  ```
  `ChatMessageController.sendMessage`가 세션 속성(`SessionConst.LOGIN_MEMBER`)에서 로그인 정보를 꺼내며, 없으면 `ChatMessageException("로그인이 필요합니다.")`을 던집니다(단, 위 버그 4번으로 클라이언트는 별도 에러 채널 없이 그냥 실패합니다).
- 구독(서버 → 클라이언트): `/topic/chat/rooms/{chatRoomId}`
  ```json
  {
    "messageId": 15,
    "roomId": 1,
    "senderId": 5,
    "senderNickname": "me",
    "content": "네고 가능할까요?",
    "sentAt": "2026-07-22T10:05:00"
  }
  ```
  (`ChatMessageResponse` — record라 정상 직렬화됩니다.)
- 메시지 수신 시 서버는 `ChatRoom.containsMember`로 발신자 권한을 검사한 뒤 `ChatMessage`를 DB에 저장하고, `chatRoom.updateLastMessageAt(...)`으로 마지막 메시지 시각을 갱신한 뒤 같은 방을 구독 중인 클라이언트 전원에게 그대로 브로드캐스트합니다.
- 에러 채널(`/topic/chat/rooms/{roomId}/errors` 같은 별도 채널)은 없습니다 — 발행 중 예외가 나면 `ChatMessageException` 핸들러 부재(버그 4번)로 클라이언트는 서버 로그 없이 조용히 실패합니다.

---

### OFFER 메시지 — 구조화된 가격 제안 (미구현, 확정 설계)

**상태**: 미구현(확정 설계) — 상세 구현 방법은 [`docs/ORDER_LIFECYCLE_GUIDE.md`](../ORDER_LIFECYCLE_GUIDE.md) 8절 참고. 프론트(`frontend/src/context/ChatSocketContext.jsx`, `frontend/src/components/ChatBubble.jsx`, `frontend/src/pages/ChatRoom.jsx`)는 이미 아래 destination/필드명을 전제로 구현되어 있습니다.

`ChatMessage`에 `type`(`TEXT`/`OFFER`), `offeredPrice`, `offerStatus`(`PENDING`/`ACCEPTED`/`REJECTED`), `orderId` 필드가 추가됩니다. 기존 텍스트 메시지는 `type=TEXT` 고정으로 하위호환됩니다.

**발행(클라이언트 → 서버)**

| destination | body | 설명 |
|---|---|---|
| `/app/chat/rooms/{chatRoomId}/offers` | `{ "price": 15000 }` | 구매자가 가격 제안 전송 |
| `/app/chat/rooms/{chatRoomId}/offers/{messageId}/accept` | (없음) | 판매자가 제안 수락 — 성공 시 서버가 `OrdersService.createFromNegotiation(...)`으로 `REQUESTED` 주문을 생성 |
| `/app/chat/rooms/{chatRoomId}/offers/{messageId}/reject` | (없음) | 판매자가 제안 거절 |

**구독(서버 → 클라이언트, `/topic/chat/rooms/{roomId}`로 브로드캐스트)**
```json
{
  "messageId": 20,
  "roomId": 1,
  "senderNickname": "me",
  "type": "OFFER",
  "offeredPrice": 15000,
  "offerStatus": "PENDING",
  "orderId": null,
  "sentAt": "2026-07-22T10:00:00"
}
```
수락되면 같은 메시지가 `offerStatus: "ACCEPTED"`, `orderId: <생성된 주문 id>`로 갱신되어 다시 브로드캐스트됩니다.

**권한**: `accept`/`reject`는 해당 채팅방의 판매자(`chatRoom.item.seller`)만 수행 가능. 이미 `ACCEPTED`/`REJECTED`로 처리된 메시지에 다시 액션을 시도하면 거부됩니다.

---

## 남은 작업 (2026-07-22 기준)

1. ~~`build.gradle`에 `spring-boot-starter-websocket` 추가.~~ 완료.
2. ~~`WebSocketConfig`(`@EnableWebSocketMessageBroker`)로 STOMP 엔드포인트/브로커 등록.~~ 완료.
3. `GlobalExceptionHandler.chatRoomException`의 파라미터 시그니처를 `(ChatRoomException e)` + `e.getHttpStatus()`로 고쳐서 실제로 401/403/404/409가 나가도록 수정(위 "알려진 버그" 3번).
4. `ChatMessageException`용 `@ExceptionHandler` 추가(위 "알려진 버그" 4번).
5. `PageMessageResponseDto`, `PageChatRoomResponse`, `ChatMessageDto`에 `@Getter`(또는 Lombok `@Getter`/record 전환)를 추가해 JSON 직렬화가 실제로 되는지 확인 및 수정(위 "알려진 버그" 2번).
6. `frontend/src/api/client.js`의 `createChatRoom` 호출 경로를 `/chat/rooms`에서 `/chatRooms`로 수정(위 "알려진 버그" 1번) — 그렇지 않으면 "구매 문의 · 가격제안" 버튼이 동작하지 않음.
7. `frontend/src/pages/ChatList.jsx`를 로컬 스토리지 대신 `GET /api/chatRooms/me` 호출로 전환(현재 백엔드-프론트 미연동).
8. 신규 API 경로에 `items`라는 단어가 섞이지 않도록 주의(`LoginCheckInterceptor`가 `/api/items`로 시작하는 GET을 전부 공개하므로, `/api/chatRooms/**`처럼 분리된 경로는 영향 없음 — 현재 상태 유지로 충분).
9. OFFER(가격 제안) 기능 전체 미구현 — "OFFER 메시지" 절 참고.
