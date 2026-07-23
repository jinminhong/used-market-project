# Members(회원) 백엔드 리뷰

`domain/member/**`, `web/exception/GlobalExceptionHandler.java`(회원 관련 부분)을 대상으로 한 코드 리뷰 기록. `docs/api/members.md`에 적힌 "알려진 이슈"를 실제 코드로 검증했고, 그 외 코드에서 직접 확인된 문제도 함께 정리했다. 엔드포인트 계약 자체는 [`docs/api/members.md`](api/members.md)가 기준 문서다.

## 문서상 우려 사항 검증 결과

- **응답에 password 평문 포함 — 실제로 재현됨(가입/수정 둘 다).** `MemberController.save()`(`MemberController.java:25-28`)는 서비스가 반환한 `memberId`를 버리고 요청 DTO(`MemberSaveDto`, password 필드 보유)를 그대로 echo한다. `PATCH /api/members/me`도 동일 — `MemberService.update()`(`MemberService.java:93`)가 요청 DTO(`MemberUpdateDto`)를 그대로 돌려준다. 문서의 경고는 현재도 100% 유효하다.
- **PATCH의 부분 업데이트 — 문서의 우려와 달리 정상 동작함.** `Member.updateMember()`(`Member.java:50-63`)는 각 필드를 `!= null`일 때만 반영해, 필드를 생략하면 기존 값이 유지된다. 다만 `address`는 통째로 교체되는 구조라 부분 주소 변경(예: `zipcode`만)은 불가능하다.

## 남은 이슈

### 1. 비밀번호 암호화 전혀 없음

`Member` 생성자(`Member.java:42-48`)와 `MemberService.join()`(`MemberService.java:27-31`)이 비밀번호를 인코딩 없이 그대로 저장한다. `LoginService.authenticate()`의 평문 비교와 짝을 이루는 문제([`docs/auth-review.md`](auth-review.md) 참고).

### 2. 응답 DTO 미분리로 인한 password 평문 echo

`MemberSaveDto`/`MemberUpdateDto`를 요청 전용으로 쓰고, 응답은 `MemberInfoDto`처럼 password 필드가 없는 별도 DTO로 내려주는 패턴으로 통일해야 한다.

### 3. PATCH /api/members/me에 Bean Validation이 전혀 연결되어 있지 않음

`MemberController.updateMyInfo()`가 `@Valid` 없이 `MemberUpdateDto`를 받고, DTO 자체에도 검증 애너테이션이 없다(`MemberSaveDto`는 있음과 대비). `nickname: ""` 같은 빈 문자열도 그대로 통과한다.

### 4. 회원 탈퇴(delete) 기능 자체가 없음

`MemberController`/`MemberService`/`MemberRepository` 어디에도 삭제 관련 코드가 없다. `Item.seller`, `WishList.member`가 모두 NOT NULL FK라, 단순 `deleteById`는 상품/찜이 하나라도 있는 회원에서 FK 위반으로 즉시 실패한다. 소프트 삭제 또는 연관 데이터 처리 정책을 먼저 설계해야 한다.

### 5. 아이디/닉네임 중복 체크에 TOCTOU + 처리되지 않은 예외

`MemberService.checkDuplicate()`가 `existsBy...`로 조회 후 별도로 `save()`하는 구조라 동시 요청 시 둘 다 통과하고 DB 유니크 제약에서만 걸린다. `GlobalExceptionHandler`에 `DataIntegrityViolationException` 핸들러가 없어 이 경우 500으로 노출된다(`docs/chat-review.md`가 지적한 채팅 도메인의 동일 이슈와 같은 패턴).

### 6. 에러 응답이 문서가 약속한 JSON 포맷이 아님

`GlobalExceptionHandler`의 회원 관련 핸들러가 전부 `.body(e.getMessage())`로 텍스트만 반환한다. `check-id`/`check-nickname`은 파라미터 누락 시 `MissingServletRequestParameterException`이 그대로 던져져 Spring 기본 에러 페이지로 나간다.

### 7. Address(임베디드 값 타입)를 API 계약 그대로 노출

`Address`는 JPA `@Embeddable`인데 요청/응답 DTO가 그대로 재사용한다. 영속성 타입과 API 계약이 분리되어 있지 않고, `city`/`street`/`zipcode`에 검증도 없다.

### 8. 사소한 코드 정리 대상

- `MemberService.findAll()`/`findByLoginId()`는 어디서도 호출되지 않는 죽은 코드(`LoginService`는 `MemberRepository`를 직접 주입받음).
- `Member.java`에 미사용 `import lombok.Data` 존재.
- `MemberService.update()`의 로그인ID 동일성 체크는 항상 참이 되는 방어 코드라 의도를 설명하는 주석이 없으면 오해하기 쉽다.

## 잘 되어 있는 부분

- `Member` 엔티티가 `@Data` 대신 `@Getter` + 명시적 `updateMember()`로 변경 지점을 좁혀둔 설계.
- `updateMember()`의 null 체크 기반 부분 업데이트는 실제로 올바르게 동작(문서 우려와 달리 문제 없음).
- `POST /api/members`는 `@Valid` + `@NotEmpty`가 실제로 연결되어 필수값 누락이 제대로 400으로 막힌다.
- `ShopInfoDto`는 엔티티를 그대로 노출하지 않고 DTO로 변환해 내려주는 등 DTO 분리 원칙이 부분적으로 잘 지켜짐(password echo 문제와 대비).
- `MemberRepository`는 필요한 조회 메서드만 최소한으로 노출하는 간결한 인터페이스.

## 다음에 손대면 좋을 순서

1. 응답 DTO 분리로 password 평문 노출 차단 — 가장 시급하고 가장 쉬운 수정.
2. 비밀번호 암호화 도입 — 회원가입/수정/로그인 세 지점에 `PasswordEncoder` 일관 적용(`docs/auth-review.md`와 함께 진행).
3. PATCH /api/members/me에 Bean Validation 연결.
4. `DataIntegrityViolationException` → 409 매핑 추가(채팅 도메인과 동일 이슈이므로 공통 처리 고려).
5. 회원 탈퇴 기능 설계 및 구현 — 연관 데이터(Item/WishList/Orders/ChatRoom) 정리 정책을 먼저 결정.
6. 에러 응답을 `{"error", "message"}` JSON 포맷으로 전환 — 프로젝트 전역 이슈이나 회원 도메인 문서가 가장 구체적으로 계약을 명시해 둠.
7. (우선순위 낮음) Address를 API 전용 DTO로 감싸기, 죽은 코드/미사용 import 정리.
</content>
