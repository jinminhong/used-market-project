# Wishlist(위시리스트) 백엔드 리뷰

`domain/wishlist/**`를 대상으로 한 코드 리뷰 기록. 기준 문서는 [`docs/api/wishlist.md`](api/wishlist.md)다.

## 남은 이슈

### 1. 찜 여부/추가/해제 응답이 `Map<String,String>`이라 `wished`가 문자열로 내려옴 — boolean DTO로 바꾸는 건 매우 간단

`WishListController.java:30-51`의 세 메서드(`findItemWished`, `addWishList`, `deleteWishList`)가 전부 `Map.of("itemId", "" + itemId, "wished", "" + wished)`처럼 boolean/Long을 문자열 결합으로 끼워 넣는다. 특별한 설계 의도가 아니라 `Map.of(String,String)`을 쓰다 보니 생긴 결과다. `record WishListStatusResponse(Long itemId, boolean wished) {}` 하나만 추가하고 세 메서드의 반환 타입만 바꾸면 된다 — 서비스 레이어는 이미 `boolean`을 반환 중이라 변경 불필요. 프론트도 문자열/boolean 양쪽을 이미 허용하므로 이 리팩터링은 프론트를 깨뜨리지 않는다.

### 2. `WishListPageResponseDto`에 판매자 `memberId`가 없는 이유 — 원인이 명확하고 수정도 간단

`WishListRepositoryImpl.java:32-33`의 QueryDSL select가 `item.seller.nickName`만 포함하고 `item.seller.id`는 빠뜨렸다. 형제 리포지토리인 `ItemRepositoryImpl.java:31-35`는 동일 패턴에서 `item.seller.id`를 이미 select에 넣고 있다. `WishListResponseDto`에 `Long memberId` 필드를 추가하고 select에 `item.seller.id`만 더하면 끝나는 수준의 변경이다 — 새 join이나 쿼리 구조 변경 불필요.

### 3. `findAllWishList`가 `item.seller`/`item.thumbnailImage`에 명시적 `.join()`이 없음 (N+1은 아니지만 스타일 불일치)

JPQL 단일값 연관관계 경로 탐색은 Hibernate가 자동으로 SQL JOIN으로 컴파일하므로 런타임 N+1은 발생하지 않는다. 다만 `ItemRepositoryImpl`은 명시적 `.join()`을 쓰는 반면 여기는 안 써서 스타일이 불일치한다. 2번 항목을 손볼 때 같이 통일하면 좋다.

### 4. 찜 추가의 동시성 — 서비스 레이어 `existsBy` 사전 체크 + DB unique 제약이 이중으로 있지만, 제약 위반이 500으로 샌다

`WishList`에 `(members_id, item_id)` 유니크 제약이 있어 DB 레벨 방어선은 존재하지만, `WishListService.addWishList`는 "먼저 조회, 그 다음 저장"하는 비원자적 패턴이라 동시 요청 시 두 번째 저장이 `DataIntegrityViolationException`을 던질 수 있다. `GlobalExceptionHandler`에 이를 잡는 핸들러가 없어 의도한 409 대신 500이 나간다(`docs/chat-review.md`의 `ChatRoom` 유니크 제약과 동일한 종류의 이슈, 우선순위도 낮음).

### 5. 상품 삭제 시 위시리스트 FK 처리가 없음 — "고아 row"가 아니라 삭제 자체가 500으로 실패할 수 있음

`Item`↔`WishList` 간에는 연관관계/cascade 선언이 전혀 없고, `WishList.item`은 NOT NULL FK다. `ItemService.delete`는 사전 정리 없이 바로 삭제하므로, 누군가 찜해 놓은 상품을 판매자가 삭제하려 하면 FK 제약 위반으로 삭제 자체가 실패하고 이 예외 역시 매핑이 없어 500으로 노출된다. 인기 상품 삭제 시나리오에서 실제로 재현 가능한 문제라 4번보다 먼저 볼 가치가 있다.

## 잘 되어 있는 부분

- `WishList` 엔티티에 `(members_id, item_id)` 유니크 제약이 있어 서비스 레이어 체크가 뚫려도 DB가 최종적으로 중복 찜을 막아준다.
- `findAllWishList`가 `limit(size+1)` + `SliceImpl` 트릭으로 `hasNext`를 계산하는 패턴이 `ItemRepositoryImpl.searchItems`와 완전히 일관적으로 적용되어 있다.
- 존재하지 않는 상품/찜 데이터를 `ItemException`/`WishListNotFoundException`으로 명확히 구분해 던지고, `GlobalExceptionHandler`가 이를 404/409로 정확히 매핑한다.
- 모든 엔드포인트가 `@Login LoginMember`로 인증된 회원 ID만 사용해 다른 회원의 위시리스트를 조회/조작할 수 있는 경로가 없다.

## 다음에 손대면 좋은 순서

1. `Map<String,String>` → boolean/Long 필드를 가진 전용 DTO로 교체 — 가장 간단하고 리스크가 낮다.
2. `WishListResponseDto`/`WishListRepositoryImpl`에 `item.seller.id`(memberId) 추가 — 프론트 상점 링크 기능의 선행 조건. 겸사겸사 명시적 `.join()`으로 `ItemRepositoryImpl`과 스타일 통일.
3. `GlobalExceptionHandler`에 `DataIntegrityViolationException` → 409 핸들러 추가 — 찜 추가 레이스와 상품 삭제 시 FK 위반 두 이슈를 한 번에 완화.
4. 상품 삭제 시 연관 위시리스트 처리 정책 결정 — "삭제 전 위시리스트 일괄 삭제" 또는 "찜한 사람이 있으면 삭제 금지(판매 중지만 허용)" 중 하나를 정해 `ItemService.delete`에 반영.
</content>
