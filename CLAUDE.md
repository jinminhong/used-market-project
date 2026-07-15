# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Korean used-goods marketplace (중고 거래 플랫폼): Spring Boot backend (`src/main/java`) + a React/Vite frontend (`frontend/`) that talks to it over `/api`.

## Commands

Backend (run from repo root, Windows):

```powershell
.\gradlew.bat bootRun        # run the Spring Boot app on :8080
.\gradlew.bat test           # run tests (JUnit platform)
.\gradlew.bat build          # compile + test + package
```

The backend requires a running H2 **TCP** server (`spring.datasource.url=jdbc:h2:tcp://localhost/~/used-market` in `src/main/resources/application.properties`) — it is not embedded/in-memory, so an H2 server process must be started separately before `bootRun`, or the datasource connection will fail. H2 console is enabled at `/h2-console`.

Frontend (run from `frontend/`):

```powershell
npm install
npm run dev       # Vite dev server on :5173, proxies /api -> http://localhost:8080
npm run build
npm run preview   # serves the production build on :4173
```

Normal local dev = two terminals: one running `gradlew.bat bootRun` from the repo root, one running `npm run dev` from `frontend/`. There is no single command that starts both.

There is currently no real frontend test suite and only a default context-load smoke test on the backend (`src/test/java/com/side/project/ProjectApplicationTests.java`) — don't assume test coverage exists for a change; verify manually via the running app.

## Architecture

### Backend package layout (`src/main/java/com/side/project`)

Each business domain under `domain/` follows the same shape: `Entity`, `*Controller`, `*Service`, `*Repository` (Spring Data JPA, no service interfaces — concrete classes only), plus a `*dto/` subpackage for request/response DTOs.

- `domain/item` — `Item` entity (name, price, `status` enum SELLING/RESERVED/SOLD, `category` enum OUTER/TOP/BOTTOM/BAG/SHOES/ACCESSORY/ETC), `ItemController` at `/api/items*`.
- `domain/itemimage` — `ItemImage` entity (belongs to one `Item`), `ItemImageController` serves stored files at `GET /api/images/{storedFilename}`. `domain/itemimage/file/FileStore` writes uploaded files to disk at the path in `file.dir` (`application.properties`) using UUID-based filenames.
- `domain/member` — `Member` entity (`loginId`/`nickName` unique, embedded `Address`), `MemberController` at `/api/members*`.
- `web/login` — `LoginController`/`LoginService`: session-cookie auth. **No Spring Security, no JWT** — login is a hand-rolled `HttpSession` check with a plaintext password comparison in `LoginService.authenticate()`.
- `web/argumentresolver` — `@Login` annotation + `LoginMemberArgumentResolver` injects the session's `LoginMember` directly into controller method parameters. It resolves to `null` (not an exception) when there's no session, so any handler using `@Login LoginMember` must be reachable only through routes the interceptor already protects, or must null-check.
- `web/interceptor/LoginCheckInterceptor` — blocks all `/**` except an exclude list configured in `web/WebConfig` (`/`, `/api/login`, `/api/logout`, `/api/members`, `/css/**`, `/error`, `/api/images/**`), **plus** a hardcoded bypass for any `GET` request whose URI contains the substring `"items"`. When adding new endpoints, be aware this substring check is not a proper path-pattern match — check both the interceptor and `WebConfig`'s exclude list to know what's actually public.
- `web/exception` — `GlobalExceptionHandler` (`@RestControllerAdvice`) maps a handful of custom `RuntimeException` subclasses (one per domain, e.g. `ItemException`, `MemberException`, `LoginFailException`, `UnauthorizedException`) to HTTP statuses. There is no handler for `MethodArgumentNotValidException` or generic exceptions — unhandled cases fall through to Spring Boot's default error response.
- `InitDb.java` (root package) — `ApplicationRunner` that seeds dummy members/items on startup; runs every time since `spring.jpa.hibernate.ddl-auto=create` drops and recreates all tables on every boot.
- `src/main/resources/templates/` — legacy Thymeleaf views (`HomeController` serves `/`) that exist in parallel to the REST API; the REST API under `/api` is what the React frontend actually uses.

Validation (`@Valid`) is applied inconsistently across controllers — check whether a given DTO's Bean Validation annotations are actually wired to `@Valid`/`@Validated` before relying on them; several currently are not.

### Frontend (`frontend/src`)

Deliberately minimal, single-file structure — there is no `components/`/`pages/` split (as of this writing):

- `App.jsx` — contains everything: the API client (`createApi`, mock-vs-real toggle), data normalizers (`normalizeItem`, `normalizeMember`, `normalizeShop`), and all page-level components (`Nav`, `Home`, `Detail`, `Shop`, `Auth`, `Editor`) as local functions, plus the root `App` component.
- `main.jsx` — mounts `<App/>`, imports `styles.css`.
- `styles.css` — single global stylesheet (no CSS modules/Tailwind/CSS-in-JS).
- No routing library — `App` keeps a `route` string in `useState` (`"home"|"detail"|"shop"|"auth"|"new"|"edit"`) and manually drives `window.history.pushState`/`popstate`; URLs never change to reflect the current view (no deep-linking).
- The item list/shop pages use infinite scroll (a `scroll` event listener triggers the next page fetch), not conventional pagination controls.

**Mock mode**: a checkbox in the header toggles `useMock`, which swaps `createApi()` between hitting the real backend (via the Vite `/api` proxy) and an in-memory mock (`sampleItems`) — useful for frontend-only work without the backend running.

**Image URLs**: item images are never embedded directly; the frontend always resolves them to `/api/images/{storedFileName}` and lets the backend serve the file from disk.

### API shape currently implemented (source of truth: `frontend/README.md`, not `docs/REST_API_FRONTEND_PLAN.md`)

`docs/REST_API_FRONTEND_PLAN.md` is an early design doc and does **not** match the current implementation (e.g. it shows `title`/`sellerId`/`{"items":[...],"total":n}`, while the real API uses `name`/`member` and returns `{"list":[...],"hasNext":bool}` for slice-based pagination). Treat `frontend/README.md`'s endpoint list as current:

```
POST   /api/login
POST   /api/members
GET    /api/members/me
PATCH  /api/members/me
GET    /api/members/{memberId}/shop
GET    /api/items?page=0&size=10        -> { "list": [...], "hasNext": bool }
GET    /api/items/{itemId}
POST   /api/items                        multipart: itemSaveDto (JSON blob) + multipartFiles
PATCH  /api/items/{itemId}               multipart: itemUpdateDto (JSON blob) + multipartFiles
DELETE /api/items/{itemId}
GET    /api/images/{storedFilename}
```

No wishlist, cart, notifications, search endpoint, or order/transaction system exist in the backend — don't assume they're available when building frontend features.
