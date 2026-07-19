# Chat API — 채팅 (전부 미구현, 신규 도메인)

이 도메인은 컨트롤러/엔티티/의존성이 **전혀 존재하지 않습니다**(`build.gradle`에 WebSocket/STOMP 의존성도 없음). 아래는 `docs/BACKEND_ROADMAP.txt`(단계 4)에 정리된 설계를 근거로 한 제안 계약입니다. 실제 구현 시 이 문서를 계약 삼아 프론트/백엔드가 함께 맞춰나가면 됩니다.

## 신규 엔티티 (제안)

- `ChatRoom`: id, item(`@ManyToOne`), seller(`@ManyToOne Member`), buyer(`@ManyToOne Member`), createdDate. `(item_id, buyer_id)` 조합 unique — 같은 상품에 같은 구매자가 채팅방을 중복 생성하지 않도록.
- `ChatMessage`: id, chatRoom(`@ManyToOne`), sender(`@ManyToOne Member`), content, sentAt.

---

### POST /api/items/{itemId}/chatrooms (채팅방 생성/조회)

**상태**: 미구현(제안)

**요청**
- 인증 필요. Path: `itemId`. Body 없음.
- 이미 해당 상품에 대해 내가 연 채팅방이 있으면 새로 만들지 않고 기존 방을 그대로 반환(멱등).

**성공 응답**
```json
{
  "roomId": 1,
  "itemId": 10,
  "itemName": "item1",
  "sellerNickName": "seller1",
  "buyerNickName": "me"
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 404 | `not_found_item` | "상품을 찾을 수 없습니다." | 존재하지 않는 `itemId` |
| 409 | `conflict_state` | "본인 상품에는 채팅방을 열 수 없습니다." | 판매자 본인이 자기 상품에 채팅 시도 |

---

### GET /api/chatrooms/me (내 채팅방 목록)

**상태**: 미구현(제안)

**요청**
- 인증 필요. Query(옵션): `page`(기본 0), `size`(기본 10).

**성공 응답**
```json
{
  "list": [
    {
      "roomId": 1,
      "itemId": 10,
      "itemName": "item1",
      "thumbnailFilename": "stored-item1-image1.jpg",
      "counterpartNickName": "seller1",
      "lastMessage": "네고 가능할까요?",
      "lastMessageAt": "2026-07-19T10:00:00"
    }
  ],
  "hasNext": false
}
```
다른 목록 API와 동일하게 `{list, hasNext}` Slice 패턴을 따릅니다.

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |

---

### GET /api/chatrooms/{roomId}/messages (메시지 이력)

**상태**: 미구현(제안)

**요청**
- 인증 필요(해당 방의 buyer/seller만). Path: `roomId`. Query(옵션): `page`, `size`.

**성공 응답**
```json
{
  "list": [
    {
      "messageId": 1,
      "senderNickName": "me",
      "content": "네고 가능할까요?",
      "sentAt": "2026-07-19T10:00:00"
    }
  ],
  "hasNext": false
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 403 | `forbidden` | "이 채팅방에 접근할 권한이 없습니다." | 방의 buyer/seller가 아닌 제3자가 조회 시도 |
| 404 | `not_found_chatroom` | "채팅방을 찾을 수 없습니다." | 존재하지 않는 `roomId` |

---

### STOMP 실시간 메시지 (제안)

REST가 아니라 WebSocket/STOMP이므로 별도로 정리합니다.

- 핸드셰이크: 기존 `LoginCheckInterceptor`(HTTP 전용)는 재사용 불가. 별도 `HandshakeInterceptor`가 HTTP 세션의 로그인 정보를 WebSocket 세션에 복사해야 하며, 세션이 없으면 핸드셰이크 자체를 거부(연결 실패)합니다.
- 발행(클라이언트 → 서버): `/pub/chat/message`
  ```json
  { "roomId": 1, "content": "네고 가능할까요?" }
  ```
- 구독(서버 → 클라이언트): `/sub/chat/room/{roomId}`
  ```json
  {
    "messageId": 15,
    "roomId": 1,
    "senderNickName": "me",
    "content": "네고 가능할까요?",
    "sentAt": "2026-07-19T10:05:00"
  }
  ```
- 메시지 수신 시 서버는 `ChatMessage`를 DB에 저장한 뒤 같은 방을 구독 중인 클라이언트 전원에게 그대로 브로드캐스트합니다.
- 에러(제안): 방에 속하지 않은 사용자가 발행/구독을 시도하면 연결을 끊거나, `/sub/chat/room/{roomId}/errors` 같은 별도 에러 채널로 `{"error": "forbidden", "message": "이 채팅방에 접근할 권한이 없습니다."}`를 보내는 방식 중 택일이 필요합니다(REST의 HTTP 상태 코드 개념이 없으므로 구현 시 정해야 함).

## 이 도메인을 시작하려면 필요한 사전 작업 (참고)

1. `build.gradle`에 `spring-boot-starter-websocket` 추가.
2. `WebSocketConfig`(`@EnableWebSocketMessageBroker`)로 STOMP 엔드포인트/브로커 등록.
3. `GlobalExceptionHandler`에 403(`forbidden`) 매핑 추가(현재 401만 존재).
4. 신규 API 경로에 `items`라는 단어가 섞이지 않도록 주의(`LoginCheckInterceptor`가 `/api/items`로 시작하는 GET을 전부 공개하므로, 예를 들어 `/api/items/{itemId}/chatrooms`에 대한 POST는 영향이 없지만 향후 GET을 추가한다면 경로를 `/api/chat-rooms`처럼 분리하는 것이 안전).
