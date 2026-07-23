# Items(상품)+Images 백엔드 리뷰

`domain/item/**`, `domain/itemimage/**`, `web/config/QuerydslConfig.java`를 대상으로 한 코드 리뷰. 계약 문서는 [`docs/api/items.md`](api/items.md), [`docs/api/images.md`](api/images.md)이며, 두 문서가 "알려진 이슈"로 적어둔 항목을 실제 코드로 재검증했다.

## 문서의 "알려진 이슈" 재검증 결과 (전부 여전히 유효함)

- **PATCH의 `status` 임의 변경** — 여전히 유효. `Item.updateItem`이 `itemUpdateDto.getStatus()`가 null이 아니면 그대로 반영하고, `ItemService.update`는 이를 걸러내지 않는다. 소유자는 주문 흐름과 무관하게 `SOLD`/`RESERVED`를 자유롭게 바꿀 수 있다.
- **DELETE 204에 문자열 바디 포함** — 여전히 유효. `ItemController.delete`가 `ResponseEntity.status(NO_CONTENT).body("삭제 완료")`를 반환한다.
- **이미지 400/404에 바디 없음** — 여전히 유효. `ItemImageController.image`가 `.badRequest().build()`/`.notFound().build()`만 반환한다.
- **이미지 경로 탈출 방어는 실제로 되어 있음** — `storedFilename`의 `..`/`/`/`\` 블랙리스트 + `Paths.get(...).normalize()` 이중 방어, UUID 기반 저장 파일명이라 문제 없음.
- **QueryDSL `searchItems`의 N+1 우려는 실제로는 없음** — `QItemListDto` 프로젝션으로 `item`/`member`/`thumbnailImage`를 한 쿼리에서 select하고 DTO를 반환하므로 지연 로딩이 없다.

## 남은 이슈

### 1. PATCH/DELETE 권한 오류의 HTTP 상태 코드가 문서와 다르고, 의미상으로도 틀렸다

- `ItemService.update`가 소유자가 아니면 `ItemException`을 던지는데 이는 무조건 404로 매핑된다. 문서(403 forbidden)와 실제(404)가 다르다.
- `ItemService.delete`가 소유자가 아니면 `UnauthorizedException`(원래 "로그인 안 됨"을 뜻하는 예외)을 재사용해 401로 나간다. 역시 문서(403)와 다르다.
- 두 엔드포인트 모두 소유자 검증 로직 자체는 존재하지만, 401/403/404 의미가 뒤섞여 프론트가 신뢰성 있게 구분할 수 없다.

### 2. 상품 저장/수정 시 이미지 파일이 등록되지만 클린업이 전혀 없다

- `ItemService.save`가 `fileStore.storeFiles(...)`로 파일을 먼저 디스크에 쓴 뒤 `itemRepository.save(item)`을 호출한다. `@Transactional`은 DB만 롤백하므로 저장 실패 시 파일이 고아로 남는다.
- `ItemService.update`의 `deletedFileIds` 처리는 DB의 `ItemImage` 행만 지우고(`orphanRemoval=true`), `FileStore`에 파일을 지우는 메서드 자체가 없어 실제 파일은 디스크에 영구적으로 남는다.
- `ItemService.delete`도 마찬가지 — DB 행은 cascade로 정리되지만 물리 파일은 전혀 삭제되지 않는다.
- 근본 원인은 `FileStore`에 `deleteFile(String storedFilename)` 메서드 자체가 없다는 것.

### 3. 상품 삭제 시 `WishList`/`ChatRoom`/`Orders`와의 FK 정합성이 깨질 수 있다

`WishList.item`/`ChatRoom.item`이 NOT NULL FK이고, `Item` 쪽에는 대응하는 역방향 매핑/cascade가 전혀 없다. `ItemService.delete`가 사전 점검 없이 바로 삭제하므로, 위시리스트에 담겼거나 채팅방이 열린 상품을 삭제하면 DB 참조 무결성 위반이 발생할 가능성이 높다. `GlobalExceptionHandler`에 `DataIntegrityViolationException` 핸들러가 없어 500으로 노출된다.

### 4. 파일 업로드에 확장자/타입 화이트리스트가 없다

`FileStore.storeFile`이 확장자 검증 없이 원본 확장자를 그대로 붙여 저장한다. `.svg`/`.html` 등을 업로드하면 `ItemImageController`가 추론한 `Content-Type`으로 그대로 서빙되어 저장형 XSS 가능성이 있다. `extractExt`도 `.`가 없는 파일명이면 파일명 전체를 확장자로 취급하는 사소한 버그가 있다.

### 5. `ItemController`/DTO에 선언된 Bean Validation이 실제로 적용되지 않는다

`saveItem`/`patchItem` 모두 `@RequestPart` DTO에 `@Valid`가 없다. `ItemSaveDto`의 `@NotEmpty name`/`@NotNull price`가 무시되어, 문서가 약속한 "400 상품명을 입력해주세요" 응답은 실제로 발생하지 않고 대신 DB 제약 위반(500)으로 끝날 가능성이 높다. `ItemUpdateDto`에는 검증 애너테이션이 아예 없다.

## 잘 되어 있는 부분

- `ItemRepositoryImpl.searchItems`의 `Slice + limit(size+1)` 트릭과 null-safe한 `BooleanExpression` 조건 분리가 깔끔하다.
- `Item.addItemImage`/`removeItemImage`가 양방향 연관관계 편의 메서드로 잘 캡슐화돼 있고, 썸네일 자동 지정 로직도 합리적이다.
- 이미지 서빙 경로의 경로 탈출 방어는 블랙리스트 + `normalize()` 이중 방어로 충분하다.
- `ItemRepository.findByIdWithMemberForUpdate`(비관적 락)가 이미 존재해 동시성 제어 필요성을 인지하고 설계된 흔적이 있다(다만 `ItemService.update`는 락 없는 `findById`를 써서 PATCH 경로가 이를 우회함 — 이슈 1과 함께 참고).
- `keywordLike`가 상품명뿐 아니라 판매자 닉네임까지 검색하는 건 UX 관점에서 합리적이다.

## 다음에 손대면 좋을 순서

1. **`FileStore`에 `deleteFile(String storedFilename)` 추가하고, `update`의 `deletedFileIds` 처리와 `delete`에서 실제 파일 삭제까지 연결** — 계속 누적되는 디스크 누수 문제라 우선순위가 가장 높다.
2. **상품 삭제 전 `WishList`/`ChatRoom`/`Orders` 참조 존재 여부 확인 정책 결정** — 삭제 차단(409)할지 연관 레코드까지 정리할지 정하고, 최소한 `DataIntegrityViolationException` 핸들러부터 추가.
3. **PATCH/DELETE의 권한 오류를 전용 403 예외로 분리** — `orders.md`의 "PATCH에서 status 필드 제거" 작업과 함께 처리하면 효율적.
4. **업로드 파일 확장자/MIME 화이트리스트 추가**(jpg/jpeg/png/webp 등) — 저장형 XSS 가능성 제거.
5. **`ItemController`의 `@RequestPart` DTO에 `@Valid` 적용** — 이미 DTO에 붙은 애너테이션을 동작시키는 것뿐이라 비용 대비 효과가 크다. `MethodArgumentNotValidException` 핸들러는 이미 있어 추가 인프라 불필요.
6. (우선순위 낮음) `keyword` 검색의 `LIKE '%...%'` 양쪽 와일드카드는 인덱스를 타지 못한다 — 데이터 규모가 커지면 풀텍스트 검색 도입 검토.
7. (우선순위 낮음) `ItemService`에 주입된 `ItemImageRepository`가 어디서도 호출되지 않는 죽은 의존성 — 정리하거나 1번 작업 시 실제로 활용.
</content>
