# API 명세서 인덱스

이 디렉토리는 URL 분기(도메인)별로 나눈 REST API 계약 문서입니다. 도메인마다 구현된 엔드포인트의 핵심 요청/응답 형태와 알려진 이슈만 간단히 정리했습니다. 백엔드에 구현되지 않은 기능은 현재 없습니다(즉시구매/상태전이/운송장/위시리스트/채팅 전부 구현 완료). 각 도메인을 실제로 더 개선하려면 아래 "도메인별 코드리뷰" 문서를 먼저 확인하세요.

이 문서가 이 저장소의 API 형태에 대한 기준 문서입니다. 현재 남아있는 개선 방향은 아래 "도메인별 코드리뷰" 문서들을 기준으로 삼으세요.

## 문서 읽는 법

각 엔드포인트는 아래 형식으로 적혀 있습니다.

```
### {METHOD} {path}
상태: 구현됨 / 미구현(제안)

요청 — 프론트가 (실제로 또는 앞으로) 보내는 것
성공 응답 — 프론트가 받고 싶은 JSON
에러 응답 — HTTP 상태별 error 코드 + 메시지 예시
```

- **상태: 구현됨** — 백엔드 코드가 이미 있음. "성공 응답"은 프론트가 기대하는 형태 그대로이며, 만약 실제 백엔드 응답이 이와 다르면 바로 아래에 "⚠️ 현재 실제로는 이렇게 옵니다"를 병기했습니다.
- **상태: 미구현(제안)** — 백엔드 코드가 아직 없음. 요청/응답 예시는 "이렇게 만들어졌으면 좋겠다"는 제안이며, 그대로 구현해도 되고 협의해서 바꿔도 됩니다. 여러 방식이 가능한 경우 옵션 A/B로 병기했습니다.

## 에러 응답 규격 (모든 도메인 공통)

```json
{ "error": "not_found_item", "message": "상품을 찾을 수 없습니다." }
```

`error`는 소문자 snake_case 코드, `message`는 사용자에게 보여줄 한글 문장입니다. 공통으로 쓰는 코드:

| error 코드 | HTTP 상태 | 의미 |
|---|---|---|
| `invalid_request` | 400 | 요청 형식/파라미터가 잘못됨 (Bean Validation 실패 등) |
| `unauthorized` | 401 | 로그인이 필요한데 인증 토큰이 없거나(만료/위조 포함) 로그인 실패 |
| `forbidden` | 403 | 로그인은 했지만 이 리소스에 대한 권한 없음(예: 남의 상품 수정) |
| `not_found_item` / `not_found_member` / `not_found_order` 등 | 404 | 대상 리소스를 찾을 수 없음 |
| `duplicate_member` / `duplicate_nickname` / `duplicate_wishlist` 등 | 409 | 이미 존재하는 리소스와 충돌 |
| `conflict_state` | 409 | 현재 상태에서 허용되지 않는 전이/동작 시도 (예: 이미 판매된 상품 구매, 잘못된 순서의 주문 상태전이) |

이 에러 JSON 포맷은 실제로 적용되어 있습니다. `web/exception/GlobalExceptionHandler`가 도메인 예외들의 공통 부모인 `ApplicationException`(및 그 하위 `ErrorCode`)을 기준으로 `{error, message}` 바디를 생성합니다(`web/exception/ErrorCode`, `web/exception/ApplicationException`, `web/exception/ErrorResponse` 참고). Bean Validation 실패(`MethodArgumentNotValidException`)와 비관적 락 충돌(`PessimisticLockingFailureException`)도 각각 `invalid_request`/`conflict_state`로 매핑되어 나간다.

## 공통 규칙

- Base URL: React에서는 `/api`. 로컬 개발 시 Vite가 `http://localhost:5173/api` → `http://localhost:8080/api`로 프록시.
- 인증: Spring Security는 없지만 JWT는 있다. `HttpSession`이 아닌 Access Token(`Authorization: Bearer ...`) + Refresh Token(HttpOnly 쿠키) 기반 수제 인증(`web/login`, `web/jwt`, `domain/auth`). 인증이 필요한 요청은 `fetch`에 `Authorization` 헤더(Access Token)와 `credentials: "include"`(Refresh 쿠키 동봉용)가 모두 필요.
- 인터셉터: `web/interceptor/LoginCheckInterceptor` + `web/config/WebConfig`가 `/**` 전체를 대상으로 동작(order 1). Authorization 헤더의 JWT를 검증해 통과 여부를 결정한다(세션 아님).
  - `excludePathPatterns`: `/`, `/api/login`, `/api/logout`, `/api/token/refresh`, `/api/members`, `/api/members/check-id`, `/api/members/check-nickname`, `/api/members/*/shop`, `/css/**`, `/error`, `/api/images/**`
  - 추가 하드코딩 규칙: **HTTP method가 GET이고 요청 URI가 `/api/items` 또는 `/api/items/**`에 매치하면** 로그인 없이 통과(`AntPathMatcher` 기반, `excludePathPatterns`가 아니라 인터셉터 내부 코드로 처리됨).
- 토큰: Access Token 유효기간 30분, Refresh Token 14일(DB 저장, rotation + 재사용 탐지). 자세한 설정 키/발급 흐름은 [auth.md](./auth.md) 참고. 로그아웃은 `POST /api/logout`이 Refresh Token을 무효화한다.
- 페이징 응답 패턴: 목록 API는 대부분 `{"list": [...], "hasNext": boolean}` 형태(Slice 방식, `total`/`totalPages` 없음).

## 도메인별 문서

| 파일 | URL 분기 | 상태 |
|---|---|---|
| [auth.md](./auth.md) | `/api/login`, `/api/logout`, `/api/token/refresh` | 구현됨 |
| [members.md](./members.md) | `/api/members/**` | 구현됨 |
| [items.md](./items.md) | `/api/items/**` | 구현됨 |
| [images.md](./images.md) | `/api/images/**` | 구현됨 |
| [orders.md](./orders.md) | `/api/orders/**` | 즉시구매/목록/상태전이(PATCH)/운송장 등록 전부 구현됨 |
| [wishlist.md](./wishlist.md) | `/api/wishlist/**` | 조회/추가/해제 전부 구현됨 |
| [chat.md](./chat.md) | `/api/chat/rooms/**`, STOMP | 채팅방 생성/목록/이력 + 실시간 메시지 + 가격 제안 생성/수락/거절 전부 구현됨 |

## 도메인별 코드리뷰 (다음에 나아가야 할 방향)

각 도메인의 실제 코드를 읽고 정리한 리뷰 문서. 계약(요청/응답)은 위 `docs/api/*.md`가, "무엇을 더 고쳐야 하는가"는 아래 문서가 담당한다.

| 파일 | 도메인 |
|---|---|
| [auth-review.md](../auth-review.md) | 로그인/세션 |
| [members-review.md](../members-review.md) | 회원 |
| [items-review.md](../items-review.md) | 상품 + 이미지 |
| [orders-review.md](../orders-review.md) | 구매/판매(주문) |
| [wishlist-review.md](../wishlist-review.md) | 위시리스트 |
| [chat-review.md](../chat-review.md) | 채팅(WebSocket/STOMP) |
