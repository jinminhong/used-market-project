# Images API (`/api/images/**`)

소스: `domain/itemimage/ItemImageController.java`, `FileStore.java`

코드리뷰(개선 방향)는 [`docs/items-review.md`](../items-review.md)에 함께 정리되어 있다(item 도메인과 파일 저장소를 공유).

---

### GET /api/images/{storedFilename}

**상태**: 구현됨. 인증 불필요. 다른 도메인 응답의 `storedFileName`/`thumbnailFilename` 값을 그대로 붙여 호출하면 이미지 바이너리를 반환한다. 경로 탈출 방어(`..`/`/`/`\` 차단 + `normalize()`)는 되어 있다.

⚠️ **알려진 이슈**: 400/404 응답에 바디가 없다(빈 응답). 업로드 파일 확장자/MIME 화이트리스트가 없어 저장형 XSS 가능성이 있다. 상세: `items-review.md`.
</content>
