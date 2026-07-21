# 주문 상태전이 구현 가이드 (백엔드 미구현 — 구현 시 참고용)

이 문서는 **코드가 아직 없는 상태**에서, 이후 백엔드를 구현할 때 그대로 옮겨 적을 수 있도록 상태 머신·엔티티·API·예외·스케줄러를 구체적으로 정리한 것이다. 프론트엔드(`frontend/src/api/client.js`, `frontend/src/context/ChatSocketContext.jsx` 등)는 이미 아래 계약을 전제로 구현되어 있으므로, 백엔드 구현 시 **필드명/엔드포인트/STOMP destination을 반드시 그대로 맞춰야 한다** (임의로 이름을 바꾸면 프론트가 깨진다).

배경: 한국형 중고거래 플랫폼(당근마켓/번개장터/후루츠패밀리 벤치마킹)에서 "채팅으로 가격 흥정 → 예약 → 결제 → 배송 → 구매확정" 흐름과 "정가 즉시구매" 흐름을 병행 지원한다. 실제 PG/택배 API 연동은 없으므로 "결제"와 "배송"은 상태값으로만 시뮬레이션한다.

## 0. 현재 코드 상태 (2026-07-22 기준, 변경 없음)

- `Orders` 엔티티(`src/main/java/com/side/project/domain/orders/Orders.java`): `id`, `buyer`, `item`, `orderStatus` 4개 필드뿐. 생성 메서드 `createOrders(buyer, item, orderStatus)` 하나만 존재, 상태전이 메서드 없음.
- `OrderStatus`(`.../orders/OrderStatus.java`): `REQUESTED, ACCEPTED, CANCELED, PAY_COMPLETED, COMPLETED` 5개 정의돼 있으나 `OrdersService.save()`가 항상 `PAY_COMPLETED`로만 생성 — 나머지는 죽은 값.
- `OrdersController`: `POST /api/orders/{itemId}`(즉시구매), `GET /api/orders/purchases`, `GET /api/orders/sales` 3개뿐. PATCH 없음.
- `ChatMessage`(`.../chat/chatmessage/ChatMessage.java`): `id`, `chatRoom`, `sender`, `content`, `sentAt`뿐. 가격 제안 필드 없음.
- `Item.updateItem(ItemUpdateDto)`에 `if (status != null) this.status = status;` 뒷문 존재 — 판매자가 범용 수정 API로 상태를 임의로 바꿀 수 있음.
- `GlobalExceptionHandler`: 도메인 예외 1개당 `@ExceptionHandler` 1개, 상태코드 고정 매핑 패턴(`ItemException`→404, `MemberException`→404, `LoginFailException`/`UnauthorizedException`→401, `WishListException`→409, `WishListNotFoundException`→404). 403 개념이 아직 없음.

## 1. 상태 머신 확정안

`OrderStatus`에 `SHIPPING` 1개만 신규 추가한다(택배 API가 없어 "배송중"과 "배송완료"를 구분할 실신호가 없으므로 하나로 병합; 대신 아래 취소 권한 규칙으로 벤치마킹 취지를 살린다). `REQUESTED`(채팅 가격합의 완료)와 `ACCEPTED`(판매자의 정식 주문 승인)는 의미를 분리해 둘 다 실제로 쓰이게 한다.

| From | 트리거 | 주체 | To | `Item.status` |
|---|---|---|---|---|
| (없음) | `POST /api/orders/{itemId}` 즉시구매 | 구매자 | `PAY_COMPLETED` | `SELLING→RESERVED` |
| (없음) | 채팅 OFFER 카드 "수락" | 판매자 | `REQUESTED` | `SELLING→RESERVED` |
| `REQUESTED` | `PATCH {action:"ACCEPT"}` | 판매자 | `ACCEPTED` | 유지 |
| `ACCEPTED` | `PATCH {action:"PAY"}` | 구매자 | `PAY_COMPLETED` | 유지 |
| `PAY_COMPLETED` | `PATCH {action:"SHIP"}` | 판매자 | `SHIPPING` | 유지 |
| `SHIPPING` | `PATCH {action:"CONFIRM"}` | 구매자 | `COMPLETED` | `RESERVED→SOLD` |
| `REQUESTED`/`ACCEPTED` | `PATCH {action:"CANCEL"}` | 구매자 또는 판매자(자유취소) | `CANCELED` | `RESERVED→SELLING` |
| `PAY_COMPLETED` | `PATCH {action:"CANCEL"}` | **판매자만**(동의) 또는 시스템(자동취소) | `CANCELED` | `RESERVED→SELLING` |
| `SHIPPING` | `PATCH {action:"CANCEL"}` | **판매자만**(귀책사유) | `CANCELED` | `RESERVED→SELLING` |
| `COMPLETED` | `CANCEL` | 불가 | 409 | - |
| 그 외 역행/스킵 | - | - | 409 | - |

바로구매와 네고흐름은 `PAY_COMPLETED` 이후(`SHIP`→`CONFIRM`)부터 완전히 합류하므로 컨트롤러/서비스 분기가 필요 없다.

## 2. `Orders` 엔티티 변경

```java
// Orders.java
@Column(name = "agreed_price")
private Integer agreedPrice; // 협상가. 없으면(즉시구매) item.getPrice()를 그대로 세팅

public void createOrders(Member buyer, Item item, OrderStatus orderStatus) {
    this.buyer = buyer;
    this.item = item;
    this.orderStatus = orderStatus;
    this.agreedPrice = item.getPrice(); // 기존 즉시구매 하위호환: 정가를 협상가로 취급
}

public void createNegotiatedOrder(Member buyer, Item item, Integer agreedPrice) {
    this.buyer = buyer;
    this.item = item;
    this.orderStatus = OrderStatus.REQUESTED;
    this.agreedPrice = agreedPrice;
}

public void accept()  { requireStatus(OrderStatus.REQUESTED);     this.orderStatus = OrderStatus.ACCEPTED; }
public void pay()     { requireStatus(OrderStatus.ACCEPTED);      this.orderStatus = OrderStatus.PAY_COMPLETED; }
public void ship()    { requireStatus(OrderStatus.PAY_COMPLETED); this.orderStatus = OrderStatus.SHIPPING; }
public void confirm() { requireStatus(OrderStatus.SHIPPING);      this.orderStatus = OrderStatus.COMPLETED; }
public void cancel()  {
    if (this.orderStatus == OrderStatus.COMPLETED || this.orderStatus == OrderStatus.CANCELED) {
        throw new OrdersConflictException("취소할 수 없는 주문입니다.");
    }
    this.orderStatus = OrderStatus.CANCELED;
}

private void requireStatus(OrderStatus expected) {
    if (this.orderStatus != expected) {
        throw new OrdersConflictException("잘못된 순서의 상태 변경입니다.");
    }
}
```

`OrderStatus.java`에 `SHIPPING` 추가:
```java
public enum OrderStatus {
    REQUESTED, ACCEPTED, CANCELED, PAY_COMPLETED, SHIPPING, COMPLETED
}
```

**권한(누가 호출 가능한지)은 엔티티가 아니라 `OrdersService`에서 검사한다** — `Item.changeStatus`가 상태값만 갱신하고 호출 권한은 서비스가 판단하는 기존 관례를 그대로 따른 것.

## 3. `OrdersService` 변경

```java
@Transactional
public OrdersActionResponseDto changeStatus(Long orderId, String action, Long requesterId) {
    Orders orders = ordersRepository.findByIdFetchItemAndBuyer(orderId)
            .orElseThrow(() -> new OrdersException("주문을 찾을 수 없습니다."));
    Item item = orders.getItem();
    boolean isBuyer = orders.getBuyer().getId().equals(requesterId);
    boolean isSeller = item.getSeller().getId().equals(requesterId);
    if (!isBuyer && !isSeller) {
        throw new OrdersForbiddenException("이 주문을 처리할 권한이 없습니다.");
    }

    switch (action) {
        case "ACCEPT" -> { requireRole(isSeller, "판매자만 승인할 수 있습니다."); orders.accept(); }
        case "PAY"    -> { requireRole(isBuyer, "구매자만 결제할 수 있습니다.");   orders.pay(); }
        case "SHIP"   -> { requireRole(isSeller, "판매자만 발송 처리할 수 있습니다."); orders.ship(); }
        case "CONFIRM"-> { requireRole(isBuyer, "구매자만 구매확정할 수 있습니다."); orders.confirm(); item.changeStatus(ItemStatus.SOLD); }
        case "CANCEL" -> {
            OrderStatus before = orders.getOrderStatus();
            if (before == OrderStatus.PAY_COMPLETED || before == OrderStatus.SHIPPING) {
                requireRole(isSeller, "판매자 동의가 필요합니다.");
            }
            orders.cancel();
            item.changeStatus(ItemStatus.SELLING);
        }
        default -> throw new OrdersConflictException("알 수 없는 action 입니다.");
    }
    return new OrdersActionResponseDto(orders.getId(), item.getId(), orders.getOrderStatus(), item.getStatus());
}

private void requireRole(boolean allowed, String message) {
    if (!allowed) throw new OrdersForbiddenException(message);
}

@Transactional
public Long createFromNegotiation(Long itemId, Long buyerId, Integer agreedPrice) {
    // save(itemId, memberId)와 동일한 SELLING 검증 + 본인상품 검증 + 비관적 락 재사용
    Item item = itemRepository.findByIdWithMemberForUpdate(itemId)
            .orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다."));
    if (!item.getStatus().equals(ItemStatus.SELLING)) throw new ItemException("구매할 수 없는 상품입니다.");
    if (item.getSeller().getId().equals(buyerId)) throw new ItemException("본인이 등록한 상품은 구매할 수 없습니다.");
    Member buyer = memberRepository.findById(buyerId).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

    Orders orders = new Orders();
    orders.createNegotiatedOrder(buyer, item, agreedPrice);
    ordersRepository.save(orders);
    item.changeStatus(ItemStatus.RESERVED);
    return orders.getId();
}

// 자동취소 (3-6절 스케줄러에서 호출)
@Transactional
public void autoCancelStalePayments(Duration threshold) {
    LocalDateTime cutoff = LocalDateTime.now().minus(threshold);
    for (Orders orders : ordersRepository.findStalePayCompleted(cutoff)) {
        orders.cancel();
        orders.getItem().changeStatus(ItemStatus.SELLING);
    }
}
```

`OrdersActionResponseDto`는 신규 record(`domain/orders/ordersdto/OrdersActionResponseDto.java`):
```java
public record OrdersActionResponseDto(Long orderId, Long itemId, OrderStatus status, ItemStatus itemStatus) {}
```

## 4. `OrdersController` 변경

```java
@PatchMapping("/{orderId}")
public ResponseEntity<OrdersActionResponseDto> changeOrderStatus(
        @PathVariable Long orderId,
        @RequestBody OrderActionRequest request,
        @Login LoginMember loginMember
) {
    return ResponseEntity.ok(ordersService.changeStatus(orderId, request.action(), loginMember.getMemberId()));
}
```

`OrderActionRequest`(신규 record, `domain/orders/ordersdto/OrderActionRequest.java`):
```java
public record OrderActionRequest(String action) {}
```

요청/응답 예시는 `docs/api/orders.md`의 `PATCH /api/orders/{orderId}` 섹션(확정판) 참고.

## 5. 예외 신규 3종 + `GlobalExceptionHandler`

기존 "예외 1개=상태코드 1개 고정" 패턴을 그대로 따른다(새 추상화 없음).

```java
// web/exception/orders/OrdersException.java        (404)
// web/exception/orders/OrdersForbiddenException.java (403)
// web/exception/orders/OrdersConflictException.java  (409)
// 각각 RuntimeException 상속, 메시지 하나만 받는 생성자
```

`GlobalExceptionHandler`에 추가:
```java
@ExceptionHandler(OrdersException.class)
public ResponseEntity ordersException(OrdersException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
}

@ExceptionHandler(OrdersForbiddenException.class)
public ResponseEntity ordersForbiddenException(OrdersForbiddenException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
}

@ExceptionHandler(OrdersConflictException.class)
public ResponseEntity ordersConflictException(OrdersConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
}
```

**같은 김에 처리할 것**: `ChatRoomException`/`ChatMessageException`도 현재 핸들러가 전혀 없어 REST 경로(`ChatRoomController`)에서 500으로 새고 있다. 네고 기능이 추가되면 예외 발생 빈도가 늘어나므로 이 시점에 404/403 매핑을 함께 추가한다.

## 6. `Item.java` 뒷문 제거

```java
// updateItem(ItemUpdateDto) 안에서 아래 블록을 삭제
if (itemUpdateDto.getStatus() != null) {
    this.status = itemUpdateDto.getStatus();
}
```

`ItemUpdateDto.java`에서 `private ItemStatus status;` 필드도 함께 삭제. `Item.status`는 이제 오직 `Item.changeStatus(...)`를 통해서만, 오직 `OrdersService`(및 자동취소 스케줄러)에서만 호출되도록 강제한다.

> 프론트는 이미 `Editor.jsx`의 상태 select와 `normalize.js`의 `itemDto.status` 주입 로직을 제거해 두었다(백엔드가 `status` 필드를 지우기 전에도 안전 — 현재 로직은 "없으면 기존 값 유지"라 하위호환됨).

## 7. 자동취소 스케줄러

```java
// ProjectApplication.java
@EnableScheduling
@SpringBootApplication
public class ProjectApplication { ... }
```

```java
// domain/orders/OrdersAutoCancelScheduler.java
@Component
@RequiredArgsConstructor
public class OrdersAutoCancelScheduler {
    private final OrdersService ordersService;

    @Value("${orders.auto-cancel.days:3}")
    private long autoCancelDays;

    @Scheduled(cron = "0 0 * * * *") // 매시 정각
    public void cancelStaleOrders() {
        ordersService.autoCancelStalePayments(Duration.ofDays(autoCancelDays));
    }
}
```

`OrdersRepository`에 추가:
```java
@Query("select o from Orders o where o.orderStatus = com.side.project.domain.orders.OrderStatus.PAY_COMPLETED and o.lastModifiedDate < :cutoff")
List<Orders> findStalePayCompleted(@Param("cutoff") LocalDateTime cutoff);
```

(`Orders`가 `BaseEntity`를 상속해 `lastModifiedDate`가 이미 있는지 확인 — 없다면 `@LastModifiedDate` 필드 추가 필요.)

`application.properties`에 `orders.auto-cancel.days=3` 추가(기본 3일, 후루츠패밀리류 벤치마킹 값).

## 8. 채팅 OFFER 구조화

**중요**: 아래 필드명/STOMP destination은 프론트(`frontend/src/context/ChatSocketContext.jsx`, `frontend/src/components/ChatBubble.jsx`)가 이미 이 이름으로 구현되어 있으므로 반드시 그대로 맞출 것.

`ChatMessage.java`에 추가:
```java
@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
private MessageType type = MessageType.TEXT; // TEXT, OFFER

private Integer offeredPrice; // type=OFFER일 때만

@Enumerated(EnumType.STRING) @Column(length = 20)
private OfferStatus offerStatus; // PENDING, ACCEPTED, REJECTED — type=OFFER일 때만

@Column(name = "order_id")
private Long orderId; // OFFER 수락으로 생성된 주문 id. 연관관계 없이 단순 컬럼

public static ChatMessage createOffer(ChatRoom chatRoom, Member sender, Integer price) {
    ChatMessage message = new ChatMessage();
    message.chatRoom = chatRoom;
    message.sender = sender;
    message.content = price + "원에 제안합니다";
    message.sentAt = LocalDateTime.now();
    message.type = MessageType.OFFER;
    message.offeredPrice = price;
    message.offerStatus = OfferStatus.PENDING;
    return message;
}

public void acceptOffer(Long orderId) {
    if (type != MessageType.OFFER || offerStatus != OfferStatus.PENDING) throw new ChatMessageException("처리할 수 없는 제안입니다.");
    this.offerStatus = OfferStatus.ACCEPTED;
    this.orderId = orderId;
}

public void rejectOffer() {
    if (type != MessageType.OFFER || offerStatus != OfferStatus.PENDING) throw new ChatMessageException("처리할 수 없는 제안입니다.");
    this.offerStatus = OfferStatus.REJECTED;
}
```

`MessageType`/`OfferStatus`는 `domain/chat/chatmessage` 아래 신규 enum 파일 2개.

`ChatMessageService.java`에 추가:
```java
public void sendOffer(Long roomId, Long senderId, Integer price) { ... } // 기존 sendMessage와 동일한 방 소속 검증 후 ChatMessage.createOffer 저장
public void acceptOffer(Long roomId, Long messageId, Long sellerId) {
    // sellerId == chatRoom.getItem().getSeller().getId() 검증
    Long orderId = ordersService.createFromNegotiation(chatRoom.getItem().getId(), chatRoom.getBuyer().getId(), chatMessage.getOfferedPrice());
    chatMessage.acceptOffer(orderId);
}
public void rejectOffer(Long roomId, Long messageId, Long sellerId) { ... }
```

`ChatMessageController.java`에 STOMP 매핑 3개 추가 (destination은 프론트 고정값):
```java
@MessageMapping("/chat/rooms/{chatRoomId}/offers")
@MessageMapping("/chat/rooms/{chatRoomId}/offers/{messageId}/accept")
@MessageMapping("/chat/rooms/{chatRoomId}/offers/{messageId}/reject")
```
각각 처리 후 기존과 동일하게 `/topic/chat/rooms/{chatRoomId}`로 브로드캐스트.

## 9. `OrdersRepositoryImpl` / DTO 변경

`findAllPurchases`/`findAllSales`의 필터를 확장:
```java
orders.orderStatus.in(REQUESTED, ACCEPTED, PAY_COMPLETED, SHIPPING, COMPLETED) // CANCELED만 제외
```

`OrdersResponseDto`(프론트가 이미 `orderStatus`, `price`(=agreedPrice) 필드를 기대함)에 `orderStatus` 필드를 추가하고, `price` 값의 소스를 `item.price`에서 `orders.agreedPrice`로 변경.

## 10. 구현 순서 체크리스트

1. `OrderStatus.SHIPPING` 추가, `Orders.agreedPrice` + 전이 메서드, 예외 3종 + `GlobalExceptionHandler` 매핑, `OrdersService.changeStatus`, `OrdersController` PATCH.
2. `Item` 뒷문 제거(`ItemUpdateDto.status` 삭제, `Item.updateItem` 분기 삭제).
3. 자동취소 스케줄러.
4. 채팅 OFFER 구조화(`ChatMessage` 필드/메서드, `ChatMessageService`, STOMP 매핑, `ChatRoomException`/`ChatMessageException` 핸들러).
5. `OrdersRepositoryImpl`/`OrdersResponseDto`에 `orderStatus`/`agreedPrice` 노출.
6. 프론트 회귀 확인(아래 11절).

## 11. 검증 방법

- H2 TCP 서버 기동 후 `.\gradlew.bat bootRun`.
- 즉시구매 → `PATCH .../{orderId} {SHIP}` → `{CONFIRM}` 순서로 Postman 호출, `Item.status`가 `RESERVED→SOLD`로 바뀌는지, `Orders.orderStatus` 전이가 표대로 되는지 확인.
- 잘못된 순서(예: `REQUESTED`에서 바로 `SHIP`)로 호출 시 409가 오는지 확인.
- `PAY_COMPLETED` 상태에서 구매자가 직접 취소 시도 → 403, 판매자가 취소 → 200 확인.
- 채팅방에서 OFFER 전송 → 판매자 수락 → `REQUESTED` 주문 생성 확인 → `ACCEPT→PAY→SHIP→CONFIRM` 전체 경로를 실제 프론트(`frontend/src/pages/ChatRoom.jsx`, `SalesHistory.jsx`, `PurchaseHistory.jsx`)에서 수동으로 밟아 확인 — 이 프론트 화면들은 이미 이 계약을 전제로 구현되어 있다.
- 자동취소 스케줄러는 `orders.auto-cancel.days`를 짧게 설정해 수동 트리거 후 H2 콘솔에서 `CANCELED`로 바뀌는지 확인.
- `ItemUpdateDto`에서 `status` 필드 제거 후 `Editor.jsx`의 다른 필드 수정이 정상 동작하는지 회귀 확인.
