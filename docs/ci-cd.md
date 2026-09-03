# CI/CD 가이드

GitHub Actions 기반 CI 파이프라인, 실패 알림, 브랜치 전략을 정리한 문서입니다.

## 1. CI 파이프라인 개요

워크플로 파일: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)

### 트리거

| 이벤트 | 조건 |
|---|---|
| `push` | `main` 브랜치 |
| `pull_request` | `main` 브랜치를 대상으로 하는 PR |

`concurrency` 설정으로 같은 브랜치(ref)에 연속 푸시하면 이전 실행은 자동 취소됩니다.

### job: `build-and-test` (`ubuntu-latest`)

| 스텝 | 하는 일 |
|---|---|
| 체크아웃 | `actions/checkout@v4` |
| JDK 17 설치 | Temurin 17 + Gradle 의존성 캐시(`cache: gradle`) |
| Gradle Wrapper 검증 | `gradle/actions/wrapper-validation` — 변조된 `gradle-wrapper.jar` 차단 |
| **컴파일 (main + test)** | `./gradlew classes testClasses` — 컴파일 실패를 별도 스텝으로 분리해 원인을 빠르게 식별 |
| **빌드 + 전체 테스트** | `./gradlew build` — 패키징 + 모든 테스트 실행 |
| 테스트 리포트 업로드 | 성공/실패 무관하게 `build/reports/tests/test`, `build/test-results/test` 를 아티팩트로 보관(7일) |
| 실패 시 Discord 알림 | 위 중 하나라도 실패하면 Discord 채널로 알림(§2) |

### 테스트 실행 방식 (Testcontainers)

모든 테스트는 `@SpringBootTest` + `@Import(TestcontainersConfig.class)` 구조이고,
`TestcontainersConfig` 가 `@ServiceConnection` 으로 `pgvector/pgvector:pg16` 컨테이너를
직접 띄웁니다. 따라서:

- **러너는 `ubuntu-latest` 여야 합니다.** GitHub 호스티드 Ubuntu 러너는 Docker 가 기본 내장이라
  추가 설정이 필요 없습니다. `windows-latest` / `macos-latest` 러너는 Linux Docker 컨테이너를
  지원하지 않아 Testcontainers 가 동작하지 않습니다.
- DB 접속 정보(`spring.datasource.*`)는 `@ServiceConnection` 이 런타임에 덮어쓰므로
  `application.properties` 의 로컬 기본값(포트 5433 등)은 CI 에 영향을 주지 않습니다.
- 파일 업로드 경로(`file.dir`)는 각 테스트가 `@DynamicPropertySource` + `@TempDir` 로
  덮어쓰므로 `C:/jinmindev/...` Windows 경로가 Linux CI 를 깨지 않습니다.
- Flyway 마이그레이션 `V1__enable_pgvector_extension.sql` (`CREATE EXTENSION vector`)도
  pgvector 이미지라 정상 실행됩니다.

### 임베딩 테스트는 CI 에서 skip 됩니다

`domain/item/embedding/` 아래 3개 테스트(`ItemEmbeddingServiceTest`,
`ItemEmbeddingPipelineTest`, `ItemHybridSearchTest`)는
`@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")` 로 게이팅되어
있어, `GEMINI_API_KEY` 가 없으면 **자동으로 skip** 됩니다. 이 테스트들은 실제 Gemini API 를
호출하고 `Thread.sleep` 폴링(최대 15초)을 하므로 CI 에서는 skip 상태를 유지하는 것을 권장합니다.

굳이 CI 에서 돌리려면 저장소 secret 에 `GEMINI_API_KEY` 를 추가하고 `ci.yml` 의
`빌드 + 전체 테스트` 스텝에 아래를 추가합니다:

```yaml
      - name: 빌드 + 전체 테스트
        run: ./gradlew --no-daemon --stacktrace build
        env:
          GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
```

### 로컬에서 CI 와 동일하게 검증

```powershell
docker compose up -d      # 로컬 dev DB (테스트는 Testcontainers 를 쓰므로 필수는 아님, Docker 데몬만 켜져 있으면 됨)
.\gradlew.bat build       # CI 의 "빌드 + 전체 테스트" 와 동일
```

Testcontainers 는 Docker Desktop(또는 Docker 데몬)이 실행 중이어야 동작합니다.

## 2. 실패 알림

### 현재 설정: Discord

`ci.yml` 마지막 스텝(`실패 시 Discord 알림`)이 `sarisia/actions-status-discord@v1` 로
Discord 채널에 실패 메시지를 보냅니다. `if: failure() && env.DISCORD_WEBHOOK != ''` 조건이라
webhook secret 이 없으면 조용히 스킵됩니다.

**설정 방법:**

1. Discord: 알림 받을 채널 → 채널 편집 → 연동(Integrations) → 웹후크 → **새 웹후크** → URL 복사
2. GitHub: 저장소 → Settings → Secrets and variables → Actions → **New repository secret**
   - Name: `DISCORD_WEBHOOK`
   - Secret: 복사한 웹후크 URL

서드파티 액션을 쓰고 싶지 않다면 `curl` 로 직접 호출해도 됩니다:

```yaml
      - name: 실패 시 Discord 알림
        if: failure() && env.DISCORD_WEBHOOK != ''
        run: |
          curl -sf -H "Content-Type: application/json" \
            -d "{\"content\": \"❌ CI 실패: ${{ github.repository }} @ ${{ github.ref_name }}\n${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}\"}" \
            "$DISCORD_WEBHOOK"
```

### 다른 알림 옵션 (참고)

| 방식 | 설정 난이도 | 비고 |
|---|---|---|
| **GitHub 기본 알림** | 없음 | 워크플로 실패 시 커밋 작성자에게 이메일 + 웹 + 모바일 앱 푸시. GitHub Mobile 앱을 깔면 사실상 "문자처럼" 즉시 푸시가 온다. Settings → Notifications → "Actions" 에서 조정. **개인 프로젝트라면 이것만으로 충분한 경우가 많다.** |
| Slack | 낮음 | `slackapi/slack-github-action` + Incoming Webhook. `SLACK_WEBHOOK_URL` secret. |
| Discord | 낮음 | 현재 설정. 위 참고. |
| **SMS(문자)** | 중간, 유료 | 국내 문자 발송 API(CoolSMS, NCP SENS, 카카오 알림톡 등)를 `if: failure()` 스텝에서 `curl` 로 호출하면 가능. API 키 + 발신번호 사전 등록(통신사 심사) 필요하고 건당 과금된다. CI 실패 알림 용도로는 과하며, GitHub 모바일 푸시나 Discord 로 대체하는 편을 권장. |

SMS 예시(CoolSMS v4, 개념만):

```yaml
      - name: 실패 시 문자 발송
        if: failure()
        run: |
          curl -X POST "https://api.coolsms.co.kr/messages/v4/send" \
            -H "Authorization: HMAC-SHA256 apiKey=..., date=..., salt=..., signature=..." \
            -H "Content-Type: application/json" \
            -d '{"message":{"to":"01012345678","from":"01000000000","text":"CI 실패: ${{ github.repository }}"}}'
```

(실제로는 HMAC 서명 계산이 필요해 별도 스크립트/액션을 쓰는 게 낫다.)

## 3. 브랜치 전략 — GitHub Flow

이 프로젝트 규모(소규모 / 사실상 1인 개발)에는 **GitHub Flow** 가 적합합니다.
Git Flow(`develop` + `release` + `hotfix` 브랜치)는 릴리스 주기가 뚜렷한 팀용이라 여기선 과합니다.

### 규칙

1. **`main` 이 유일한 장수(long-lived) 브랜치**이고, 항상 배포 가능한 상태를 유지한다.
2. 모든 작업은 `main` 에서 브랜치를 따서 시작한다. 브랜치 이름 규칙:
   - `feature/<요약>` — 새 기능 (예: `feature/item-semantic-search`)
   - `fix/<요약>` — 버그 수정
   - `chore/<요약>` — 빌드/설정/의존성 등
   - `docs/<요약>` — 문서
3. 작업 → 커밋 → push → **Pull Request 생성**.
4. PR 에서 CI(`build-and-test`)가 **반드시 통과**해야 한다. (리뷰는 인원에 맞게 선택)
5. **Squash and merge** 로 병합하고, 병합된 브랜치는 삭제한다.
6. `main` 직접 push 는 금지한다(아래 브랜치 보호로 강제).
7. 릴리스가 필요하면 `main` 의 특정 커밋에 태그(`v1.0.0`)를 단다. 별도 릴리스 브랜치는 만들지 않는다.
8. 긴급 수정(hotfix)도 동일한 흐름(`fix/*` → PR → merge)을 탄다. `main` 이 항상 배포 가능하므로
   별도 hotfix 경로가 필요 없다.

### 브랜치 보호 설정 (저장소 Settings → Branches)

`main` 에 대해 브랜치 보호 규칙(또는 Ruleset)을 추가:

- ✅ **Require a pull request before merging**
  - 1인 개발이면 "Require approvals" 는 0 으로 두고 PR 자체만 강제해도 된다.
- ✅ **Require status checks to pass before merging**
  - 상태 체크로 **`build-and-test`** 선택
  - ✅ Require branches to be up to date before merging
- (선택) ✅ Do not allow bypassing the above settings
- (선택) ✅ Require linear history

### 병합 방식 설정 (저장소 Settings → General → Pull Requests)

- ✅ Allow squash merging (이것만 켜기)
- ⬜ Allow merge commits / ⬜ Allow rebase merging (끄기)
- ✅ Automatically delete head branches

## 4. 최초 도입 체크리스트

- [ ] `feature/add-ci` 브랜치에서 `.github/workflows/ci.yml` + 이 문서 커밋 후 push
- [ ] Actions 탭에서 `CI / build-and-test` 실행 성공 확인 (임베딩 테스트 3개는 `skipped` 표시가 정상)
- [ ] `DISCORD_WEBHOOK` secret 등록
- [ ] `main` 브랜치 보호 규칙 추가 (`build-and-test` 를 필수 체크로)
- [ ] Settings → General 에서 squash merge 만 허용 + 병합 후 브랜치 자동 삭제
- [ ] (알림 검증) 테스트를 일부러 깨뜨린 커밋을 push 해 Discord 알림 도착 확인 후 되돌리기
