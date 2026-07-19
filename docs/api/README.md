# API 명세서 인덱스

이 디렉토리는 URL 분기(도메인)별로 나눈 REST API 계약 문서입니다. 코드를 직접 읽지 않아도 "프론트가 무엇을 보내고, 무엇을 받고 싶어하는지"를 알 수 있도록, **모든 엔드포인트를 요청 / 성공 응답 / 에러 응답 예시 JSON**으로 정리했습니다. 아직 백엔드에 없는 기능(찜 해제, 주문 상태전이, 채팅 등)도 같은 형식으로 "제안 계약"을 작성해 두었습니다.

이 문서가 이 저장소의 API 형태에 대한 기준 문서입니다. `docs/REST_API_FRONTEND_PLAN.md`는 초기 설계 초안이라 참고하지 마세요. `docs/BACKEND_ROADMAP.txt`는 미구현 기능의 배경/설계 의도를 설명하는 로드맵 문서이며, 여기 있는 "미구현(제안)" 항목들의 근거 자료입니다.

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
| `unauthorized` | 401 | 로그인이 필요한데 세션이 없거나 로그인 실패 |
| `forbidden` | 403 | 로그인은 했지만 이 리소스에 대한 권한 없음(예: 남의 상품 수정) |
| `not_found_item` / `not_found_member` / `not_found_order` 등 | 404 | 대상 리소스를 찾을 수 없음 |
| `duplicate_member` / `duplicate_nickname` / `duplicate_wishlist` 등 | 409 | 이미 존재하는 리소스와 충돌 |
| `conflict_state` | 409 | 현재 상태에서 허용되지 않는 전이/동작 시도 (예: 이미 판매된 상품 구매, 잘못된 순서의 주문 상태전이) |

**⚠️ 중요**: 이 에러 JSON 포맷은 프론트가 받고 싶은 **목표 규격**입니다. 현재 실제 `web/exception/GlobalExceptionHandler`는 이런 JSON이 아니라, 예외 메시지 문자열을 그대로 텍스트 바디로 반환합니다(`ResponseEntity.status(...).body(e.getMessage())`). 즉 "구현됨" 엔드포인트라도 에러 응답만큼은 전부 "미구현(제안)" 상태이며, 이 포맷대로 응답하려면 `GlobalExceptionHandler`와 각 도메인 예외 클래스가 함께 수정되어야 합니다. 각 파일에서는 이 사실을 반복하지 않습니다.

## 공통 규칙

- Base URL: React에서는 `/api`. 로컬 개발 시 Vite가 `http://localhost:5173/api` → `http://localhost:8080/api`로 프록시.
- 인증: Spring Security/JWT 없음. `HttpSession` 기반 수제 로그인(`web/login`). 세션이 필요한 요청은 `fetch`에 `credentials: "include"`가 있어야 함.
- 인터셉터: `web/interceptor/LoginCheckInterceptor` + `web/config/WebConfig`가 `/**` 전체를 대상으로 동작(order 1).
  - `excludePathPatterns`: `/`, `/api/login`, `/api/logout`, `/api/members`, `/api/members/check-id`, `/api/members/check-nickname`, `/api/members/*/shop`, `/css/**`, `/error`, `/api/images/**`
  - 추가 하드코딩 규칙: **HTTP method가 GET이고 요청 URI가 `/api/items`로 시작하면** 로그인 없이 통과. 문자열 접두사 비교라 `AntPathMatcher` 기반이 아니므로, 새 경로 추가 시 의도치 않게 공개될 수 있음.
- 페이징 응답 패턴: 목록 API는 대부분 `{"list": [...], "hasNext": boolean}` 형태(Slice 방식, `total`/`totalPages` 없음).

## 도메인별 문서

| 파일 | URL 분기 | 상태 |
|---|---|---|
| [auth.md](./auth.md) | `/api/login`, `/api/logout` | 로그인 구현됨, 로그아웃 미구현(제안) |
| [members.md](./members.md) | `/api/members/**` | 구현됨 |
| [items.md](./items.md) | `/api/items/**` | 구현됨 |
| [images.md](./images.md) | `/api/images/**` | 구현됨 |
| [orders.md](./orders.md) | `/api/orders/**` | 즉시구매만 구현됨, 상태전이 미구현(제안) |
| [wishlist.md](./wishlist.md) | `/api/wishlist/**` | 조회/추가만 구현됨, 삭제 미구현(제안) |
| [chat.md](./chat.md) | `/api/items/{itemId}/chatrooms`, `/api/chatrooms/**`, STOMP | 전부 미구현(제안) — 코드 자체가 없음 |
