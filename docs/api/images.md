# Images API (`/api/images/**`)

소스: `domain/itemimage/ItemImageController.java`, `FileStore.java`

---

### GET /api/images/{storedFilename}

**상태**: 구현됨

**요청**
- 인증 불필요. Path: `storedFilename` — 다른 도메인 응답의 `storedFileName`/`thumbnailFilename` 값을 그대로 붙여서 호출.
- 예: `GET /api/images/stored-item1-image1.jpg`

**성공 응답**
- HTTP 200, `Content-Type`은 파일 확장자로 자동 추론, 바디는 이미지 바이너리 스트림(JSON 아님).

**에러 응답**

| HTTP 상태 | error 코드 | 메시지 예시 | 발생 조건 |
|---|---|---|---|
| 400 | `invalid_request` | "잘못된 파일 경로입니다." | `storedFilename`에 `..`, `/`, `\` 포함(경로 탈출 시도) |
| 404 | `not_found_image` | "이미지를 찾을 수 없습니다." | 파일이 존재하지 않거나 읽을 수 없음 |

⚠️ 현재 실제로는 400/404 모두 바디 없이 빈 응답(`ResponseEntity.badRequest().build()` / `.notFound().build()`)만 내려갑니다. 위 에러 JSON 포맷을 적용하려면 바디를 채우도록 컨트롤러 수정이 필요합니다.

프론트에서 이미지를 쓸 때는 항상 이 엔드포인트를 통해 URL을 조립하세요(`item.thumbnailFilename`이든 `itemImages[0].storedFileName`이든 동일하게 `/api/images/{그 값}`).
