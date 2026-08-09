# Auth(로그인) 백엔드 리뷰

`web/login/**`, `web/jwt/**`, `domain/auth/**`, `web/argumentresolver/**`, `web/interceptor/**`, `web/config/WebConfig`, `web/config/WebSocketConfig`를 대상으로 한 코드 리뷰 기록. 코드를 직접 읽고 확인한 내용만 담았다. 엔드포인트 계약 자체는 [`docs/api/auth.md`](api/auth.md)가 기준 문서다.

세션 기반 인증 → JWT(Access + Refresh Token) 기반, 비밀번호 평문 저장/비교 → BCrypt 해싱으로 전환하는 작업이 완료되어, 아래는 그 이후 상태를 기준으로 다시 정리한 것이다.

## 해결됨

- **비밀번호 평문 저장·비교** — `MemberService.join()`/`update()`가 `PasswordEncoder.encode()`(BCrypt)로 해싱 후 저장하고, `LoginService.authenticate()`는 `passwordEncoder.matches()`로 비교한다. `web/config/PasswordEncoderConfig`가 `BCryptPasswordEncoder` 빈을 등록한다.
- **세션 고정(session fixation) 공격** — `HttpSession`을 더 이상 생성하지 않으므로 이 공격 클래스 자체가 구조적으로 사라졌다. 인증 상태는 stateless Access Token(JWT)과 서버에 저장된 Refresh Token으로 관리된다.
- **세션 쿠키 보안 속성 미설정** — Refresh Token 쿠키는 `HttpOnly`가 항상 켜져 있고, `Secure`/`SameSite`는 `application.properties`의 `app.auth.cookie.secure`/`app.auth.cookie.same-site`로 명시적으로 관리된다(로컬은 `false`/`Lax`, 배포 시 `true`/환경에 맞게 조정 필요).
- **`LoginMemberArgumentResolver`의 로깅 오타 및 매 요청 INFO 로그** — JWT 전환 과정에서 `supportsParameter()`의 `log.info("supprotsParameter 실행")` 라인을 제거했다.
- **`jwt.secret`의 로컬 기본값이 코드에 커밋되어 있음** — 배포 환경에서 `JWT_SECRET`을 설정하지 않으면 알려진 로컬 기본값(`local-dev-only-secret-change-me-32bytes-minimum`)으로 서명되는 위험이 있었다. `JwtTokenProvider` 생성자에서 주입된 secret이 이 기본값과 같으면 `log.warn`으로 경고를 남기도록 했다 — 배포 시 `JWT_SECRET` 미설정을 부팅 로그에서 바로 확인할 수 있다.
- **에러 응답이 `{error, message}` JSON 포맷이 아니었던 문제** — `GlobalExceptionHandler`가 도메인 예외들의 공통 부모 `ApplicationException`(+ `ErrorCode`)을 기준으로 `{error, message}` JSON을 반환하도록 전 도메인(`auth`/`item`/`orders`/`member`/`wishlist`/`chat`) 예외 클래스와 함께 리팩터링됐다. `docs/api/README.md`의 목표 규격이 실제로 적용된 상태다.

## 남은 이슈

### 1. 로그인 실패에 대한 rate limiting/lockout이 전혀 없음

`LoginService.authenticate()`는 실패 횟수를 세거나 계정/IP를 잠그는 로직이 없어 브루트포스에 무방비다. 도입하려면 `Member`에 실패 카운트 컬럼을 추가하거나 별도 캐시가 필요해 범위가 크다.

### 2. 로그아웃 시 Access Token 즉시 무효화 불가

Access Token은 stateless JWT라 서버가 개별 토큰을 추적하지 않는다. 로그아웃은 Refresh Token만 무효화하므로, 이미 발급된 Access Token은 만료 시각(기본 30분, `jwt.access-token-expiration-ms`)까지 이론상 계속 유효하다. 즉시 차단이 필요하면 별도 블랙리스트(예: Redis 기반 jti 저장)가 필요하지만 현재 범위에서는 의도적으로 수용한 리스크다.

### 3. 만료/폐기된 Refresh Token 레코드가 DB에 계속 쌓임

`domain/auth/RefreshTokenService`는 rotation/로그아웃/재사용 탐지 시 `revoked_at`만 세팅할 뿐 row를 삭제하지 않는다(재사용 탐지 근거로 남겨두는 것은 의도적). 다만 만료 후 오래된 row를 정리하는 배치가 없어 `refresh_token` 테이블이 무한히 커진다. 프로젝트에 이미 `spring-boot-starter-batch-jdbc` 의존성이 있으므로, `OrdersAutoCancelScheduler`와 유사한 스케줄러/배치 잡으로 만료 row를 주기적으로 삭제하는 것을 고려할 수 있다.

## 잘 되어 있는 부분

- `LoginController.login`에 `@Valid LoginForm`이 실제로 연결되어 있고, `MethodArgumentNotValidException` 핸들러와도 잘 이어져 400을 반환한다.
- 인증 관련 예외(`LoginFailException`, `JwtAuthenticationException`, `UnauthorizedException`)가 `JwtAuthenticationException extends UnauthorizedException` 상속 관계로 일관되게 401로 매핑된다.
- `LoginController.logout`이 쿠키 유무와 무관하게 항상 204를 반환해 멱등하고 안전하다.
- Refresh Token rotation 시 재사용 탐지(reuse detection)가 구현되어 있다 — 이미 폐기된 토큰이 다시 제출되면 해당 회원의 모든 Refresh Token을 즉시 무효화한다(`RefreshTokenService.rotate()`). 무효화 로직 자체가 예외 발생 시 트랜잭션 롤백으로 함께 취소되지 않도록 `@Transactional(noRollbackFor = JwtAuthenticationException.class)`로 명시되어 있다(구현 중 실제로 걸렸던 버그).
- STOMP 채팅(`/ws-chat`)도 `StompAuthChannelInterceptor`로 CONNECT 프레임의 JWT를 검증해 HTTP REST와 인증 모델이 일관적이다(브라우저 네이티브 WebSocket 핸드셰이크는 커스텀 헤더를 못 실으므로 핸드셰이크가 아닌 STOMP 프레임 레벨에서 인증).
- `WebConfig`의 `excludePathPatterns`가 실제 공개 엔드포인트(`/api/token/refresh` 포함)와 정확히 대응된다.

## 다음에 손대면 좋을 순서

1. 로그인 실패 rate limiting/lockout 설계 — 스키마 변경 또는 별도 캐시가 필요해 범위가 큼.
2. 만료된 Refresh Token 정리 배치 추가.
