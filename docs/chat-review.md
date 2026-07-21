# 채팅(WebSocket/STOMP) 백엔드 리뷰

`domain/chat/**`, `web/config/WebSocketConfig.java`, `web/exception/chat/**`를 대상으로 한 코드 리뷰입니다. **이 문서는 기록용이며, 아래 지적된 코드는 이번 작업에서 수정하지 않았습니다.** 처음 만들어본 WebSocket/STOMP 채팅 기능의 현재 상태와 개선 방향을 정리해 다음 작업의 참고 자료로 남깁니다.

## 요약

- STOMP 브로커/목적지 체계, 엔티티 설계(FK, unique 제약)는 기본기가 탄탄합니다.
- 다만 메시지 이력 조회 기능이 미완성 상태로 주석 처리되어 있고, 예외 처리와 인증 타이밍 쪽은 보완이 필요합니다.
- 채팅방 "내 목록" 조회 API 자체가 없어, 프론트는 이번에 localStorage 기반 임시 목록으로 우회했습니다(`frontend/src/lib/chatStorage.js`).

## 이슈 목록 (심각도 순)

### 1. 메시지 이력 조회가 미완성 상태로 주석 처리됨

`src/main/java/com/side/project/domain/chat/chatroom/ChatRoomController.java:31-38`

```java
//    @GetMapping("/{roomId}/messages")
//    public ResponseEntity<Slice<ChatMessageResponse>> getMessages(
//            @PathVariable Long roomId,
//            Pageable pageable,
//            @Login LoginMember loginMember
//    ) {
//        return ResponseEntity.ok(chatRoomService.getMessages(roomId,loginMember.getMemberId(),pageable));
//    }
```

`ChatRoomService`에는 이 메서드가 대응하는 구현 자체가 없어서, 주석을 그대로 풀면 컴파일이 깨집니다. 즉 "설계는 있는데 구현을 마무리하지 못하고 주석으로 남겨둔" 상태입니다. 이 API가 없으면 채팅방에 재입장했을 때 과거 대화 내용을 불러올 방법이 없어, 클라이언트는 새로고침할 때마다 대화가 사라진 것처럼 보입니다.

**제안**: `ChatRoomService`에 `getMessages(roomId, memberId, pageable)`를 추가하되, 요청자가 해당 방의 buyer/seller인지 인가 체크(`ChatRoom.containsMember`)를 먼저 거친 뒤 `ChatMessageRepository`에 `chatRoom.id`로 페이징 조회하는 메서드를 정의해야 합니다. 지금 `ChatMessageRepository`는 빈 인터페이스라 이 조회 메서드도 함께 추가해야 합니다.

### 2. STOMP 목적지 변수 이름 불일치 (재확인 결과 문제 없음)

`src/main/java/com/side/project/domain/chat/chatmessage/ChatMessageController.java:23-24`

```java
@MessageMapping("/chat/rooms/{chatRoomId}/messages")
public void sendMessage(@DestinationVariable Long chatRoomId, ...)
```

최초 리뷰에서는 템플릿 변수 `chatRoomId`와 파라미터명 `roomId`가 달라 바인딩 실패 위험이 있다고 지적했으나, 코드를 다시 확인한 결과 파라미터명이 이미 `chatRoomId`로 템플릿 변수와 일치합니다. `@DestinationVariable`에 이름을 명시하지 않아도 파라미터 이름 기반 자동 매칭이 정상 동작하므로 조치가 필요 없습니다.

### 3. 채팅 예외에 대한 HTTP/STOMP 매핑이 전혀 없음

`src/main/java/com/side/project/web/exception/GlobalExceptionHandler.java` 전체를 확인했으나 `ChatRoomException`/`ChatMessageException`에 대한 `@ExceptionHandler`가 없습니다(`MemberException`→404, `WishListException`→409 등 다른 도메인 예외는 모두 매핑되어 있는 것과 대조적입니다). `ChatRoomController.createChatRoom`(REST)에서 `ChatRoomException`이 던져지면 매핑되지 않아 Spring 기본 500 처리로 빠집니다.

STOMP `@MessageMapping`에서 던지는 예외는 애초에 `@RestControllerAdvice`(HTTP 전용)의 대상이 아니므로, 별도로 `@MessageExceptionHandler`를 두지 않으면 클라이언트는 왜 메시지가 전송되지 않았는지 전혀 알 수 없습니다.

**제안**:
- `GlobalExceptionHandler`에 `ChatRoomException`→409, `ChatMessageException`→400 매핑 추가.
- `ChatMessageController`에 `@MessageExceptionHandler`를 추가해, 실패 시 `/user/queue/errors` 같은 개인 큐로 `{error, message}`를 돌려주는 방식을 고려하세요(STOMP는 HTTP 상태 코드 개념이 없으므로 별도 채널이 필요합니다).

### 4. WebSocket 핸드셰이크 단계에서 인증 검증이 없음

`src/main/java/com/side/project/web/config/WebSocketConfig.java:18-22`

```java
registry.addEndpoint("/ws-chat")
        .setAllowedOriginPatterns("http://localhost:5173")
        .addInterceptors(new HttpSessionHandshakeInterceptor());
```

`HttpSessionHandshakeInterceptor`는 HTTP 세션의 속성을 WebSocket 세션으로 복사만 할 뿐, 로그인 여부를 검증하지 않습니다. 따라서 비로그인 사용자도 핸드셰이크(연결) 자체는 성공하고, 실제 차단은 메시지 전송 시점(`ChatMessageController`가 `sessionAttributes`에서 `LOGIN_MEMBER`를 확인하는 부분)에서만 이루어집니다. 기능상 최종적으로는 막히지만, 인증 없는 연결이 서버 리소스를 점유할 수 있고 설계상으로도 "연결 단계에서 걸러야 할 것"이 뒤로 미뤄져 있습니다.

**제안**: `HttpSessionHandshakeInterceptor`를 커스텀 `HandshakeInterceptor`로 교체해, `beforeHandshake`에서 세션에 로그인 정보가 없으면 핸드셰이크 자체를 거부(`false` 반환)하세요.

### 5. N+1 쿼리 가능성 (재확인 결과 현재는 문제 없음)

`ChatRoomResponse.from`(`dto/ChatRoomResponse.java:17`)은 `chatRoom.getItem().getSeller().getId()`로 접근합니다. `item`(`ChatRoom.item`)과 `seller`(`Item.seller`) 모두 `LAZY` 연관관계지만:

- `findChatRoomByItemAndBuyer`/`chatRoomFetchJoinItem`(`ChatRoomRepository.java:11-15`) 둘 다 이미 `item`을 fetch join하므로, 이 경로에서 `item`은 프록시가 아니라 초기화된 실제 엔티티입니다.
- `seller`는 초기화되지 않은 프록시가 맞지만, Hibernate는 프록시 생성 시점에 식별자(FK) 값을 이미 갖고 있어서 `getId()`처럼 식별자 getter만 호출하는 경우에는 추가 SELECT 없이 그 값을 그대로 반환합니다.

따라서 `id`만 노출하는 현재 코드에는 N+1 문제가 없습니다. 지금 당장 조치는 필요 없지만, 이후 `ChatRoomResponse`가 seller의 nickname 등 id 외의 필드를 노출하게 되면 그때는 실제로 `seller`를 초기화해야 하므로 `select cr from ChatRoom cr join fetch cr.item i join fetch i.seller where ...`처럼 fetch join을 추가해야 합니다.

### 6. STOMP 메시지에 Bean Validation 미적용

`ChatMessageRequest`(`dto/ChatMessageRequest.java`)에는 `@NotBlank @Size(max=1000)`가 선언되어 있지만, `ChatMessageController.sendMessage`의 파라미터에 `@Valid`가 없어(`ChatMessageController.java:24-26`) 적용되지 않습니다. `ChatMessageService.sendMessage`가 `content.trim().isBlank()`를 자체 체크하므로 빈 문자열은 최소한 걸러지지만, 1000자 초과는 애플리케이션 레벨에서 전혀 걸러지지 않고 DB 컬럼 길이 제약(`ChatMessage.java:31`, `length = 1000`)에서야 저장 시점에 실패합니다.

**제안**: `sendMessage(@DestinationVariable ... , @Valid ChatMessageRequest request, ...)`로 `@Valid`를 추가하고, 검증 실패 시의 처리도 위 3번 항목의 `@MessageExceptionHandler`에 함께 태우세요.

### 7. 채팅방 "내 목록 조회" API 자체가 없음

`ChatRoomController`에는 방 생성(`POST`)과 (주석 처리된) 메시지 이력 조회 두 엔드포인트만 있고, 로그인한 회원이 참여 중인 채팅방 전체를 나열하는 API가 없습니다. 프론트가 채팅방 목록 화면을 만들려면 반드시 필요한 기능인데, 백엔드에 전혀 준비되어 있지 않습니다.

**제안**: `ChatRoomRepository`에 `select cr from ChatRoom cr join fetch cr.item i join fetch i.seller where cr.buyer.id = :memberId or i.seller.id = :memberId` 형태의 조회 메서드를 추가하고, `Slice` 기반으로 페이징하세요(다른 목록 API들과 `{list, hasNext}` 계약을 맞추면 프론트 통합이 쉬워집니다).

## 잘 되어 있는 부분

- STOMP 브로커 설정(`enableSimpleBroker("/topic","/queue")` + `setApplicationDestinationPrefixes("/app")`)과 실제 발행/구독 목적지(`/app/chat/rooms/{roomId}/messages`, `/topic/chat/rooms/{roomId}`) 체계가 일관적입니다.
- `ChatRoom`(`item_id, buyer_id` unique 제약)과 `ChatMessage`(FK, `nullable=false`) 엔티티 설계가 견고합니다 — 같은 상품에 같은 구매자가 중복으로 방을 만드는 것을 DB 레벨에서 막고 있습니다.
- `build.gradle:27`의 `spring-boot-starter-websocket` 의존성 추가는 STOMP 지원에 필요하고 충분합니다.
- `ChatRoomService.createChatRoom`이 판매자 본인이 자기 상품에 채팅을 걸지 못하도록 막는 검증(29-31행)과, 이미 연 방이 있으면 새로 만들지 않고 기존 방을 반환하는 멱등 처리(34-41행)는 의도한 대로 잘 되어 있습니다.
- `ChatMessageService.sendMessage`의 인가 체크(`if (!chatRoom.containsMember(senderId)) throw ...`)는 정상 동작합니다 — 방에 속하지 않은 제3자는 메시지를 보낼 수 없습니다.

## 다음에 손대면 좋을 순서 (제안)

1. `ChatRoomService.getMessages` 구현 + 주석 해제(1번) — 메시지 이력 없이는 채팅 기능이 실사용에 부적합함.
2. 예외 매핑 추가(3번) — 이후 프론트 에러 처리를 붙이기 위한 전제.
3. 채팅방 목록 조회 API(7번) — 이번엔 프론트가 localStorage로 우회했지만, 다른 기기/브라우저에서 접속하면 목록이 비어 보이는 근본 원인.
4. 핸드셰이크 인증(4번), Bean Validation(6번)은 기능 자체를 막지는 않으므로 후순위로 미뤄도 무방.
5. STOMP 파라미터 이름(2번), N+1(5번)은 재확인 결과 현재 문제가 없어 조치 불필요 — 참고용으로만 남겨둠.
