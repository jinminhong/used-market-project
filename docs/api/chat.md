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

**상태**: 구현됨(REST 엔드포인트, STOMP 아님). 채팅방이 없으면 생성 후 OFFER 메시지를 저장하고 `/topic/chat/rooms/{roomId}`로도 브로드캐스트한다.

⚠️ **미구현 범위**: 제안 **생성**까지만 되어 있고, 판매자의 **수락/거절 API는 없다**. `ChatMessage.offerStatus` 필드와 `OfferStatus`(PENDING/ACCEPTED/REJECTED) enum은 존재하지만 실제로는 어디서도 값이 대입되지 않아 항상 `null`이며, 응답 DTO(`ChatMessageResponse`/`OfferedMessageResponse`)에도 `messageType`/`offerStatus`가 노출되지 않아 프론트가 일반 메시지와 가격 제안을 구분할 수 없다. 상세 및 다음 단계는 `chat-review.md` 참고.
</content>
