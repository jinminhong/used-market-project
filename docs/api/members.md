# Members API (`/api/members/**`)

소스: `domain/member/MemberController.java`, `MemberService.java`, `domain/member/memberdto/*`, `frontend/src/api/client.js`의 `signup/checkLoginId/checkNickname/findShop/getMyInfo/updateMyInfo`

코드리뷰(개선 방향)는 [`docs/members-review.md`](../members-review.md) 참고.

---

### POST /api/members (회원가입)

**상태**: 구현됨. `loginId`/`name`/`password`/`nickname`(+옵션 `address`)를 받아 회원을 생성한다. `loginId`/`nickname` 중복 시 409, 필수값 누락 시 400.

⚠️ 응답에 **`password`가 평문 그대로 echo**된다 — 프론트는 절대 사용/저장하지 말 것(상세: `members-review.md`).

---

### GET /api/members/me

**상태**: 구현됨. 인증 필요. `{ memberId, loginId, nickName, name, address }` 반환.

---

### PATCH /api/members/me

**상태**: 구현됨. 인증 필요. 보낸 필드만 부분 수정(값을 안 보내면 기존 값 유지 — 실제로 정상 동작 확인됨). 닉네임 중복 시 409.

⚠️ 이 응답에도 마찬가지로 **`password`가 평문 echo**된다.

---

### GET /api/members/check-id?loginId=…

**상태**: 구현됨. 인증 불필요. `{ duplicate: boolean }` 반환.

---

### GET /api/members/check-nickname?nickname=…

**상태**: 구현됨. 인증 불필요. `{ duplicate: boolean }` 반환.

---

### GET /api/members/{memberId}/shop

**상태**: 구현됨. 인증 불필요. 해당 회원의 닉네임/이름과 등록 상품 목록(`itemList`)을 반환.
</content>
