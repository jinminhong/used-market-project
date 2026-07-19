# Members API (`/api/members/**`)

소스: `domain/member/MemberController.java`, `MemberService.java`, `domain/member/memberdto/*`, `frontend/src/api/client.js`의 `signup/checkLoginId/checkNickname/findShop/getMyInfo/updateMyInfo`

---

### POST /api/members (회원가입)

**상태**: 구현됨

**요청**
- 인증 불필요.
```json
{
  "loginId": "newuser",
  "name": "Hong",
  "password": "1234",
  "nickname": "newnick",
  "address": { "city": "Seoul", "street": "Road", "zipcode": "00001" }
}
```
`loginId`/`name`/`password`/`nickname` 모두 빈 값 불가.

**성공 응답**
```json
{
  "loginId": "newuser",
  "name": "Hong",
  "nickname": "newnick",
  "address": { "city": "Seoul", "street": "Road", "zipcode": "00001" }
}
```
⚠️ 현재 실제로는 위 응답에 **`password` 필드가 평문 그대로 포함**되어 옵니다(요청 DTO를 그대로 echo하는 구조라서). 프론트는 응답의 `password`를 절대 사용/저장하지 말고, 백엔드는 응답 전용 DTO로 분리해 `password`를 빼고 내려주는 것으로 고쳐야 합니다.

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 409 | `duplicate_member` | "이미 사용 중인 아이디입니다." | `loginId` 중복 |
| 409 | `duplicate_nickname` | "이미 사용 중인 닉네임입니다." | `nickname` 중복 |
| 400 | `invalid_request` | "아이디를 입력해주세요." | 필수 필드 누락 |

---

### GET /api/members/me

**상태**: 구현됨

**요청**
- 인증 필요. Body 없음.

**성공 응답**
```json
{
  "memberId": 1,
  "loginId": "user1",
  "nickName": "nickname1",
  "name": "member1",
  "address": { "city": "Seoul", "street": "Road", "zipcode": "00001" }
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 404 | `not_found_member` | "회원을 찾을 수 없습니다." | 세션은 있지만 회원 데이터가 삭제됨 등 |

---

### PATCH /api/members/me

**상태**: 구현됨

**요청**
- 인증 필요.
```json
{
  "name": "New Name",
  "password": "5678",
  "nickname": "newnick",
  "address": { "city": "Seoul", "street": "New Road", "zipcode": "00002" }
}
```
전 필드 옵션(부분 수정 의도로 보이나, 실제 부분 업데이트 동작 여부는 `MemberService.update()` 구현에 따름 — 값을 안 보내면 그대로 유지되는지 별도 확인 필요).

**성공 응답**
```json
{
  "name": "New Name",
  "nickname": "newnick",
  "address": { "city": "Seoul", "street": "New Road", "zipcode": "00002" }
}
```
⚠️ `POST /api/members`와 마찬가지로 현재 실제 응답에는 `password`가 평문으로 그대로 포함됩니다. 마찬가지로 프론트는 무시하고, 백엔드는 응답 DTO에서 제외해야 합니다.

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 401 | `unauthorized` | "로그인이 필요합니다." | 세션 없음 |
| 409 | `duplicate_nickname` | "이미 사용 중인 닉네임입니다." | 변경하려는 닉네임이 이미 존재 |

---

### GET /api/members/check-id?loginId={loginId}

**상태**: 구현됨

**요청**
- 인증 불필요. Query: `loginId`(필수).

**성공 응답**
```json
{ "duplicate": true }
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 400 | `invalid_request` | "확인할 아이디를 입력해주세요." | `loginId` 누락 |

---

### GET /api/members/check-nickname?nickname={nickname}

**상태**: 구현됨

**요청**
- 인증 불필요. Query: `nickname`(필수).

**성공 응답**
```json
{ "duplicate": false }
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 400 | `invalid_request` | "확인할 닉네임을 입력해주세요." | `nickname` 누락 |

---

### GET /api/members/{memberId}/shop

**상태**: 구현됨

**요청**
- 인증 불필요. Path: `memberId`.

**성공 응답**
```json
{
  "nickName": "nickname1",
  "name": "member1",
  "itemList": [
    {
      "itemId": 1,
      "name": "item1",
      "description": "desc",
      "price": 1100,
      "status": "SELLING",
      "category": "OUTER",
      "nickName": "nickname1",
      "uploadFileDto": { "itemImageId": 1, "originalFilename": "a.jpg", "storedFileName": "stored-a.jpg" }
    }
  ]
}
```

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 404 | `not_found_member` | "회원을 찾을 수 없습니다." | 존재하지 않는 `memberId` |
