# Auth API (`/api/login`, `/api/logout`)

소스: `web/login/LoginController.java`, `LoginService.java`, `frontend/src/api/client.js`의 `login()`

---

### POST /api/login

**상태**: 구현됨

**요청**
- 인증 불필요.
- Body:
```json
{
  "loginId": "user1",
  "password": "1234"
}
```
- Query(옵션): `redirectUrl`(기본값 `"/"`) — 현재는 받기만 하고 실제로 사용되지 않는 죽은 파라미터.

**성공 응답**
```json
{
  "memberId": 1,
  "loginId": "user1",
  "nickname": "nickname1"
}
```
로그인 성공 시 세션에도 동일한 정보가 저장됩니다. ⚠️ 필드명이 `nickName`이 아니라 **`nickname`**(소문자 n)입니다. `members.md`의 `GET /api/members/me` 응답은 `nickName`(대문자 N)을 쓰므로 두 응답을 같은 코드로 정규화할 때 주의하세요.

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "아이디 또는 비밀번호가 올바르지 않습니다." | `loginId`/`password` 불일치 |
| 400 | `invalid_request` | "로그인 아이디를 입력해주세요." | `loginId`/`password` 누락 |

```json
{ "error": "unauthorized", "message": "아이디 또는 비밀번호가 올바르지 않습니다." }
```

---

### POST /api/logout

**상태**: 미구현(제안)

현재 `WebConfig`의 인터셉터 `excludePathPatterns`에 `/api/logout`이 이미 올라가 있지만(로그인 없이도 호출 가능하도록 열려 있음), 실제로 이 경로를 처리하는 컨트롤러가 없습니다. 프론트에 로그아웃 버튼이 필요하면 아래 계약으로 신규 구현을 제안합니다.

**요청**
- 인증 불필요(로그아웃은 세션이 없어도 실패하지 않아야 함 — 이미 없는 세션에 로그아웃을 호출해도 그냥 성공 처리).
- Body 없음.

**성공 응답**
```json
{ "status": "ok" }
```
서버는 `HttpSession.invalidate()`로 세션을 무효화합니다.

**에러 응답**

이 엔드포인트는 실패할 일이 거의 없으므로(세션이 있든 없든 200) 별도 에러 코드를 두지 않는 것을 제안합니다.
