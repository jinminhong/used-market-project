# 택배 배송조회 API 연동 — 사용자가 해야 할 일

`frontend/src/api/delivery.js`는 스마트택배(SweetTracker) API 연동 골격만 준비되어 있고, 실제로 동작하려면 아래 작업이 필요합니다. API 키 발급은 코드로 대신할 수 없어 사용자가 직접 진행해야 합니다.

## 1. SweetTracker API 키 발급

1. https://tracking.sweettracker.co.kr (또는 https://info.sweettracker.co.kr ) 에서 회원가입 후 API 키(`t_key`)를 발급받습니다.
2. 무료 플랜의 호출 한도/약관을 확인합니다(트래픽이 늘어나면 유료 플랜 검토 필요).

## 2. 프론트엔드에 키 설정

`frontend/.env`(없으면 새로 생성, `.gitignore`에 포함되어 있는지 확인)에 아래처럼 추가합니다.

```
VITE_SWEETTRACKER_KEY=발급받은_키
```

키가 없으면 `delivery.js`는 하드코딩된 택배사 목록으로 폴백하고, 배송조회 함수는 `null`을 반환합니다(콘솔에 경고만 출력).

## 3. 택배사 코드 재검증

`frontend/src/api/delivery.js`의 `CARRIERS` 목록(CJ대한통운=04, 한진택배=05, 롯데택배=08, 우체국택배=01, 로젠택배=06)은 추정치입니다. 실제 SweetTracker 문서의 `companylist` API 응답과 대조해서 정확한 코드로 교체해야 합니다.

## 4. CORS 이슈 가능성

브라우저에서 SweetTracker API를 직접 호출하면 CORS 정책에 막힐 수 있습니다. 막힐 경우 백엔드에 프록시 엔드포인트(예: `GET /api/delivery/carriers`, `GET /api/delivery/tracking`)를 추가해서 백엔드가 대신 호출하도록 우회해야 합니다.

## 5. 프로덕션 키 노출 문제

Vite의 `VITE_` 접두사 환경변수는 빌드 시 번들에 그대로 포함되어 브라우저에서 노출됩니다. 무료 조회용 키라면 큰 문제가 아닐 수 있지만, 호출량 제한/과금이 걸린 키라면 프로덕션 배포 전에 3번 CORS 프록시와 함께 키를 백엔드로 옮기는 것을 고려하세요.
