# Auth(로그인) 백엔드 리뷰

`web/login/**`, `web/argumentresolver/**`, `web/interceptor/LoginCheckInterceptor`, `web/config/WebConfig`를 대상으로 한 코드 리뷰 기록. 코드를 직접 읽고 확인한 내용만 담았으며, `docs/api/auth.md`/`docs/api/README.md`에 적힌 기존 "알려진 이슈"가 현재도 유효한지 재검증했다. 엔드포인트 계약 자체는 [`docs/api/auth.md`](api/auth.md)가 기준 문서다.

## 해결됨 (문서/기존 서술과 실제 코드가 달랐던 부분)

- **`LoginCheckInterceptor`의 `/api/items` 예외가 이미 `AntPathMatcher` 기반으로 정확히 매칭됨** — `LoginCheckInterceptor.java:18,26-29`가 `PATH_MATCHER.match("/api/items", requestURI)` / `match("/api/items/**", requestURI)`를 사용한다. `CLAUDE.md`에 남아있는 "`startsWith` 하드코딩, 과거에는 `contains`였음"이라는 서술은 더 이상 코드와 일치하지 않는다. `/api/itemsFoo` 같은 무관한 경로가 오탐으로 뚫리는 문제는 없다.
- **`LoginMemberArgumentResolver`는 세션이 없거나 `LOGIN_MEMBER` 속성이 없을 때 `null`을 반환하지 않고 `UnauthorizedException`을 던진다** — `LoginMemberArgumentResolver.java:38-46`. `CLAUDE.md`의 "세션이 없으면 예외가 아니라 `null`로 해석되므로 반드시 null 체크를 해야 한다"는 서술은 현재 구현과 다르다. `@Login LoginMember`를 쓰는 모든 컨트롤러를 확인한 결과 어디에서도 null 체크를 하지 않지만 NPE 위험은 없다 — `LoginCheckInterceptor`(preHandle)와 리졸버 양쪽에서 이중으로 막고 있는 구조.

## 남은 이슈

### 1. 비밀번호가 평문으로 저장·비교됨 (가장 심각)

- 저장: `Member.java:45`(생성자), `Member.java:54-55`(`updateMember`), `MemberService.java:29`(회원가입) — 어디에도 `PasswordEncoder`/해싱 로직이 없다.
- 비교: `LoginService.java:17` — `member.getPassword().equals(loginForm.getPassword())`로 평문 문자열 비교.
- DB(H2 파일)가 유출되거나 SQL 로그(`show-sql`)에 바인딩 파라미터가 남을 경우 비밀번호가 그대로 노출된다.

### 2. 세션 고정(session fixation) 공격에 대한 방어가 없음

- `LoginController.java:28-29`에서 세션을 얻고 바로 `setAttribute`만 호출한다. 로그인 성공 시점에 세션을 재발급(`changeSessionId()` 등)하는 처리가 없어, 로그인 전 세션 쿠키를 심어두는 고전적인 session fixation이 가능하다. 수정은 인증 성공 직후 `request.changeSessionId()` 한 줄로 비교적 간단하다.

### 3. 로그인 실패에 대한 rate limiting/lockout이 전혀 없음

- `LoginService.authenticate`(`LoginService.java:15-19`)는 실패 횟수를 세거나 계정/IP를 잠그는 로직이 없어 브루트포스에 무방비다. 도입하려면 `Member`에 실패 카운트 컬럼을 추가하거나 별도 캐시가 필요해 범위가 크다.

### 4. 세션 쿠키 보안 속성이 명시적으로 설정되어 있지 않음

- `application.properties`에 `server.servlet.session.cookie.secure`/`http-only`/`same-site` 설정이 없다. 로컬 개발(HTTP)에서는 문제 없지만 배포(HTTPS) 전환 시 `Secure`/`SameSite` 명시가 필요하다.

### 5. `LoginMemberArgumentResolver`의 로깅이 매 요청마다 시끄럽고 오타가 있음

- `LoginMemberArgumentResolver.java:22`("supprotsParameter 실행" — 오타)와 `:33`이 `log.info`라서, `@Login`이 없는 요청까지 포함해 매 요청마다 INFO 로그가 쌓인다. `debug`로 낮추거나 제거 권장.

### 6. 여전히 유효한 기존 이슈

- `redirectUrl` 쿼리 파라미터(`LoginController.java:24`)는 받기만 하고 사용되지 않는 죽은 파라미터.
- `GlobalExceptionHandler`의 로그인 관련 핸들러가 여전히 `e.getMessage()`를 텍스트 바디로 반환하며, `docs/api/README.md`가 요구하는 `{"error","message"}` JSON 포맷이 아니다(전 도메인 공통 이슈).

## 잘 되어 있는 부분

- `LoginController.login`에 `@Valid LoginForm`이 실제로 연결되어 있고, `MethodArgumentNotValidException` 핸들러와도 잘 이어져 400을 반환한다.
- 인증 관련 예외(`LoginFailException`, `UnauthorizedException`)가 일관되게 401로 매핑된다.
- `LoginController.logout`이 세션 유무와 무관하게 항상 204를 반환해 멱등하고 안전하다.
- 세션 타임아웃을 7일로 늘린 이유가 주석으로 명시되어 있어 추적하기 좋다.
- `WebConfig`의 `excludePathPatterns`가 실제 공개 엔드포인트와 정확히 대응되며 `docs/api/README.md`와도 일치한다.
- WebSocket 핸드셰이크(`LoginHandshakeInterceptor`, [`docs/chat-review.md`](chat-review.md) 참고)도 동일한 세션 속성을 검사해 HTTP/STOMP 양쪽 인증 모델이 일관적이다.

## 다음에 손대면 좋을 순서

1. 로그인 성공 시 세션 재발급 추가(`request.changeSessionId()`) — 비용 대비 효과가 가장 큰 session fixation 방어.
2. 비밀번호 해싱 도입 — `Member` 생성/수정과 `LoginService.authenticate` 세 지점을 `PasswordEncoder`(BCrypt 등)로 동시에 변경.
3. 로그인 실패 rate limiting/lockout 설계 — 스키마 변경 또는 별도 캐시가 필요해 범위가 크므로 2번 다음 우선순위.
4. 세션 쿠키 `Secure`/`SameSite` 속성 명시 — 배포(HTTPS) 전환 시점에 맞춰 추가.
5. `GlobalExceptionHandler`를 `{error, message}` JSON 포맷으로 전환 — auth 전용이 아니라 전 도메인 공통 작업이므로 다른 도메인과 묶어 한 번에 정리.
6. (우선순위 낮음) `LoginMemberArgumentResolver`의 로그 레벨/오타 정리, `redirectUrl` 죽은 파라미터 제거 여부 결정.
</content>
