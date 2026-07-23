# Auth API (`/api/login`, `/api/logout`)

소스: `web/login/LoginController.java`, `LoginService.java`, `frontend/src/api/client.js`의 `login()`

코드리뷰(개선 방향)는 [`docs/auth-review.md`](../auth-review.md) 참고.

---

### POST /api/login

**상태**: 구현됨

`{ loginId, password }`를 받아 세션 기반으로 로그인한다. 성공 시 `{ memberId, loginId, nickname }`을 응답하고 동일 정보를 세션에 저장한다. 실패 시 401(`unauthorized`), 필수값 누락 시 400.

⚠️ 응답 필드명이 `nickname`(소문자 n)으로, `members.md`의 `GET /api/members/me`가 쓰는 `nickName`(대문자 N)과 다르니 정규화 시 주의.

---

### POST /api/logout

**상태**: 구현됨

세션 유무와 무관하게 항상 `204 No Content`를 반환하며, 세션이 있으면 무효화한다. 별도 에러 케이스 없음.
</content>
