# Auth API (`/api/login`, `/api/logout`, `/api/token/refresh`)

소스: `web/login/LoginController.java`, `LoginService.java`, `web/jwt/JwtTokenProvider.java`, `domain/auth/RefreshTokenService.java`, `frontend/src/api/client.js`의 `login()`/`tryRefresh()`

코드리뷰(개선 방향)는 [`docs/auth-review.md`](../auth-review.md) 참고.

세션 기반 인증에서 JWT(Access Token + Refresh Token) 기반으로 전환됨. 비밀번호도 평문 비교/저장에서 BCrypt 해싱으로 전환됨(`web/config/PasswordEncoderConfig`).

---

### POST /api/login

**상태**: 구현됨

`{ loginId, password }`를 받아 `PasswordEncoder.matches()`로 비밀번호를 검증한다. 성공 시 `{ accessToken, accessTokenExpiresIn, memberId, loginId, nickname }`을 응답 바디로, Refresh Token은 `Set-Cookie: refreshToken=...; HttpOnly; Path=/api`로 내려준다(응답 바디에는 포함되지 않음). 실패 시 401, 필수값 누락 시 400.

이후 모든 인증이 필요한 요청은 `Authorization: Bearer <accessToken>` 헤더를 포함해야 한다(더 이상 세션 쿠키에 의존하지 않음).

⚠️ 응답 필드명이 `nickname`(소문자 n)으로, `members.md`의 `GET /api/members/me`가 쓰는 `nickName`(대문자 N)과 다르니 정규화 시 주의.

---

### POST /api/token/refresh

**상태**: 구현됨

요청 바디 없음. `refreshToken` 쿠키를 읽어 검증 후 새 Access Token + 새 Refresh Token을 발급한다(rotation). 응답 형태는 `POST /api/login`과 동일한 `TokenResponse`이며, 새 `refreshToken` 쿠키가 다시 `Set-Cookie`된다.

- 이미 폐기(rotate 또는 로그아웃)된 Refresh Token이 다시 제출되면 재사용(탈취/재생) 공격으로 간주해 해당 회원의 **모든** Refresh Token을 즉시 무효화하고 401을 반환한다 — 이후 재로그인이 필요하다.
- 만료되었거나 존재하지 않는 토큰도 401.
- `web/config/WebConfig`의 인터셉터 제외 목록에 포함되어 있어, 만료된 Access Token으로도(=Authorization 헤더 없이도) 호출 가능하다.
- 프론트는 `client.js`의 401 응답 시 이 엔드포인트를 자동 호출해 재시도(1회)하며, 앱 마운트 시에도 새로고침으로 사라진 메모리상의 Access Token을 복구하기 위해 먼저 호출한다(`SessionContext.jsx`).

---

### POST /api/logout

**상태**: 구현됨

쿠키의 Refresh Token을 무효화하고 쿠키를 만료시킨다(`Max-Age=0`). 쿠키 유무와 무관하게 항상 `204 No Content`를 반환한다. Access Token은 stateless JWT라 즉시 무효화되지 않으며, 만료 시각(기본 30분)까지는 이론상 유효하다 — 의도적으로 수용한 제약이다.

---

## 토큰 설정

`application.properties`:

| 키 | 기본값 | 설명 |
|---|---|---|
| `jwt.secret` | `${JWT_SECRET:...}` | HS256 서명 키, 256bit 이상 필요. 배포 시 반드시 `JWT_SECRET` 환경변수로 override |
| `jwt.access-token-expiration-ms` | 1800000 (30분) | Access Token 유효기간 |
| `jwt.refresh-token-expiration-ms` | 1209600000 (14일) | Refresh Token 유효기간 |
| `app.auth.cookie.secure` | `false` | Refresh 쿠키 `Secure` 속성. 로컬 http 개발환경 기준값, 배포(HTTPS) 시 `true` |
| `app.auth.cookie.same-site` | `Lax` | Refresh 쿠키 `SameSite` 속성 |

Refresh Token은 JWT가 아니라 `SecureRandom` 기반 opaque 랜덤 문자열이며, DB(`refresh_token` 테이블, `domain/auth/RefreshToken`)에는 SHA-256 해시로만 저장된다.
