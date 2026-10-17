# AI 건강습관 MVP REST API 명세서

작성일: 2026-10-14  
문서 버전: v0.1 Draft  
백엔드: Java / Spring Boot / Spring MVC  
클라이언트: Android / Kotlin / Jetpack Compose  
기본 경로: `/api/v1`

## 1. 문서 목적

이 문서는 성준이 구현할 Spring Boot API와 현서가 구현할 Android 앱 사이의 통신 계약 초안을 정의한다. 실제 구현 시 OpenAPI YAML을 단일 기준으로 관리하고, 이 문서와 Swagger 화면이 일치하도록 한다.

## 2. 공통 규칙

### 전송 형식

- 프로토콜: HTTPS
- 본문: JSON
- 문자 인코딩: UTF-8
- 날짜: `YYYY-MM-DD`
- 일시: ISO 8601 시간대 포함 문자열
- 서버 저장 시간: UTC
- ID: API에서는 UUID 문자열, DB에서는 `BINARY(16)`

### 인증

보호 API는 다음 헤더를 사용한다.

```http
Authorization: Bearer {accessToken}
```

회원가입, 로그인, 소셜 로그인, 이메일 인증, 비밀번호 재설정과 공개 의약품 사전은 인증 없이 호출할 수 있다. 로그아웃, 회원 탈퇴, 개인 복약 등록과 사용자 기록은 액세스 토큰 인증이 필요하다.

### 공통 헤더

```http
Content-Type: application/json
Accept: application/json
X-Request-Id: {optional-client-request-id}
Idempotency-Key: {required-for-selected-post-requests}
```

`Idempotency-Key`는 습관 행동, 식사 생성, 복약 행동처럼 중복 저장 위험이 있는 요청에 적용한다.

## 3. 공통 응답

### 단건 성공

```json
{
  "data": {
    "id": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82af"
  },
  "traceId": "7b3470e0f0b142e7"
}
```

### 목록 성공

```json
{
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "hasNext": false
  },
  "traceId": "7b3470e0f0b142e7"
}
```

### 오류

```json
{
  "code": "VALIDATION_ERROR",
  "message": "입력값을 확인해 주세요.",
  "fieldErrors": [
    {
      "field": "doseAmount",
      "reason": "0보다 커야 합니다."
    }
  ],
  "traceId": "7b3470e0f0b142e7"
}
```

### 주요 HTTP 상태

| 상태 | 의미 |
|---:|---|
| 200 | 조회·수정 성공 |
| 201 | 생성 성공 |
| 204 | 응답 본문 없는 성공 |
| 400 | 입력 또는 업무 규칙 오류 |
| 401 | 인증 없음·만료·위조 |
| 403 | 접근 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복 또는 현재 상태 충돌 |
| 422 | 형식은 맞지만 처리할 수 없는 요청 |
| 429 | 요청 횟수 제한 |
| 500 | 서버 내부 오류 |

### 오류 코드

| 코드 | 의미 |
|---|---|
| `VALIDATION_ERROR` | 필드 검증 실패 |
| `AUTHENTICATION_REQUIRED` | 인증 필요 |
| `INVALID_CREDENTIALS` | 로그인 정보 오류 |
| `ACCESS_DENIED` | 소유권 또는 권한 없음 |
| `RESOURCE_NOT_FOUND` | 대상 없음 |
| `DUPLICATE_RESOURCE` | 중복 데이터 |
| `STATE_CONFLICT` | 허용되지 않는 상태 전환 |
| `IDEMPOTENCY_CONFLICT` | 멱등성 키 충돌 |
| `FILE_UPLOAD_FAILED` | 파일 업로드 실패 |
| `INTERNAL_ERROR` | 내부 오류 |

## 4. 공통 상태값

### 습관 회차

```text
SCHEDULED | COMPLETED | SNOOZED | SKIPPED | MISSED | CANCELLED
```

### 식사

```text
COMPLETED | SKIPPED | INCOMPLETE
```

### 복약 항목

```text
ACTIVE | PAUSED | STOPPED | COMPLETED
```

### 복약 회차

```text
SCHEDULED | TAKEN | SKIPPED | NO_STOCK | MISSED | CANCELLED
```

### 식사 관계

```text
FIXED_TIME | BEFORE_MEAL | AFTER_MEAL | EMPTY_STOMACH
```

### 약 유형

```text
PRESCRIPTION | OTC | SUPPLEMENT | SPORTS_SUPPLEMENT
```

### 식사 유형

```text
BREAKFAST | LUNCH | DINNER | SNACK | LATE_NIGHT
```

상태값은 백엔드와 Android에 동일하게 정의하고 임의의 자유 문자열을 허용하지 않는다.

## 5. API 목록

### 인증·회원

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|:---:|
| POST | `/auth/signup` | 회원가입 | N |
| POST | `/auth/email-verifications/confirm` | 이메일 인증 완료 | N |
| POST | `/auth/email-verifications/resend` | 이메일 인증 재발송 | N |
| POST | `/auth/login` | 로그인 | N |
| POST | `/auth/social` | Google·Kakao 소셜 로그인 | N |
| POST | `/auth/refresh` | 토큰 갱신 | Refresh |
| POST | `/auth/password-resets/request` | 비밀번호 재설정 요청 | N |
| POST | `/auth/password-resets/confirm` | 비밀번호 재설정 완료 | N |
| POST | `/auth/logout` | 로그아웃 | Y |
| GET | `/users/me` | 내 프로필 | Y |
| DELETE | `/users/me` | 회원 탈퇴 | Y |
| PATCH | `/users/me` | 내 프로필 수정 | Y |
| GET | `/users/me/consents` | 동의 상태 | Y |
| POST | `/users/me/consents` | 동의 저장 | Y |
| POST | `/users/me/onboarding/complete` | 기본 프로필 온보딩 완료 | Y |

2026-10-14: 위 프로필 수정·동의 조회/변경·온보딩 완료를 구현했다.
GET /users/me에 profile 객체가 추가되며 PATCH 응답은 프로필 객체다.
필드 규칙, 선택값 삭제, 동의 철회 제한 및 요청 예시는 [프로필·온보딩 API](../backend/PROFILE_ONBOARDING_API.md)를 기준으로 한다.

### 홈

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/home/today?date=YYYY-MM-DD` | 오늘의 습관·식사·복약 요약 |

### 습관

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/habits` | 내 습관 목록 |
| POST | `/habits` | 습관 생성 |
| GET | `/habits/{habitId}` | 습관 상세 |
| PATCH | `/habits/{habitId}` | 습관 수정 |
| POST | `/habits/{habitId}/stop` | 습관 중지 |
| POST | `/habits/{habitId}/resume` | 습관 재개 |
| GET | `/habit-occurrences?date=YYYY-MM-DD` | 날짜별 예정 회차 |
| POST | `/habit-occurrences/{occurrenceId}/actions` | 완료·미루기·건너뛰기 |

### 식사·음식

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/meals?from=&to=` | 기간별 식사 목록 |
| POST | `/meals` | 식사 생성 |
| GET | `/meals/{mealId}` | 식사 상세 |
| PATCH | `/meals/{mealId}` | 식사 수정 |
| DELETE | `/meals/{mealId}` | 식사 삭제 또는 취소 |
| GET | `/foods?keyword=&page=` | 음식 검색 |
| POST | `/foods` | 사용자 음식 등록 |
| POST | `/files/meal-images` | 식사 이미지 업로드 |

### 의약품 사전

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|:---:|
| GET | `/drugs?keyword=&page=&size=` | 의약품 검색 | N |
| GET | `/drugs/{drugId}` | 의약품 상세 | N |

구현된 입력 범위·정렬·빈 결과·오류 응답은 [의약품 사전 API](../backend/DRUG_DICTIONARY_API.md)를 기준으로 한다.
공공 데이터 적재 구현에서 상세 응답에 imageUrl·interactions 필드를 추가했다. 적재는 운영자 시작 설정으로 실행하며 사용자용 적재 API는 없다. 실제 데이터 적용 여부는 출처와 적재 이력으로 확인한다.

### 개인 복약

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/medications` | 개인 복약 목록 |
| POST | `/medications` | 개인 복약 생성 |
| GET | `/medications/{medicationId}` | 개인 복약 상세 |
| PATCH | `/medications/{medicationId}` | 개인 복약 수정 |
| POST | `/medications/{medicationId}/stop` | 복용 중지 |
| POST | `/medications/{medicationId}/complete` | 복용 종료 |
| GET | `/medication-occurrences?date=` | 날짜별 예정 복용 회차 |
| POST | `/medication-occurrences/{occurrenceId}/actions` | 복용 행동 기록 |

### 알림

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/notification-settings` | 내 알림 설정 |
| PUT | `/notification-settings` | 알림 설정 저장 |
| POST | `/device-tokens` | 푸시 토큰 등록 |
| DELETE | `/device-tokens/{tokenId}` | 푸시 토큰 해제 |

### 통계

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/statistics/summary?from=&to=` | 통합 기본 통계 |
| GET | `/statistics/habits?from=&to=` | 습관 완료율 |
| GET | `/statistics/meals?from=&to=` | 영양 합계 |
| GET | `/statistics/medications?from=&to=` | 복약 이행률 |

### 분석 이벤트

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/analytics/events` | 단일 또는 묶음 이벤트 수신 |

## 6. 주요 API 상세

### 6.1 회원가입

```http
POST /api/v1/auth/signup
```

```json
{
  "email": "user@example.com",
  "password": "example-password",
  "consents": [
    {
      "type": "TERMS_OF_SERVICE",
      "version": "2026-10",
      "agreed": true
    },
    {
      "type": "PRIVACY",
      "version": "2026-10",
      "agreed": true
    },
    {
      "type": "SENSITIVE_HEALTH_DATA",
      "version": "2026-10",
      "agreed": true
    }
  ]
}
```

```json
{
  "data": {
    "userId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82af",
    "status": "PENDING_EMAIL_VERIFICATION",
    "verificationRequired": true
  },
  "traceId": "7b3470e0f0b142e7"
}
```

가입 직후에는 토큰을 발급하지 않는다. 이메일 인증 완료 후 액세스 토큰과 갱신 토큰을 발급한다.

### 6.2 로그인

```http
POST /api/v1/auth/login
```

```json
{
  "email": "user@example.com",
  "password": "example-password"
}
```

응답은 `userId`, `accessToken`, `refreshToken`, `expiresInSeconds`를 포함한다.

### 6.3 이메일 인증 완료

```http
POST /api/v1/auth/email-verifications/confirm
```

```json
{ "token": "one-time-email-token" }
```

성공하면 로그인과 동일한 토큰 응답을 반환한다. 토큰은 일회용이며 만료되거나 이미 사용된 토큰은 거절한다. 동시 제출은 한 번만 성공하며, 새 메일 발급 시 이전 링크를 무효화한다. 계정별 재발송 간격은 기본 60초다. GET /verify-email 화면에서 버튼을 눌러 POST로 인증하며 링크를 열기만 해서는 토큰을 소비하지 않는다.

### 6.4 소셜 로그인

```http
POST /api/v1/auth/social
```

```json
{
  "provider": "GOOGLE",
  "token": "provider-token",
  "consents": [
    { "type": "TERMS_OF_SERVICE", "version": "2026-10", "agreed": true },
    { "type": "PRIVACY", "version": "2026-10", "agreed": true },
    { "type": "SENSITIVE_HEALTH_DATA", "version": "2026-10", "agreed": true }
  ]
}
```

Google은 ID 토큰, Kakao는 액세스 토큰을 전달한다. 신규 가입 시 필수 동의가 필요하다. Google의 대상 클라이언트와 Kakao 토큰의 숫자 앱 ID를 설정값과 대조한다. 동일 이메일의 기존 계정은 자동 연결하지 않고 ACCOUNT_LINK_REQUIRED로 기존 로그인 방식 사용을 안내한다. 계정 연결 API는 미구현이다.

### 6.5 비밀번호 재설정

요청 API는 계정 존재 여부와 무관하게 동일한 `accepted: true` 응답을 반환한다. 완료 API는 `token`, `newPassword`를 받고 모든 로그인 세션을 폐기하여 기존 액세스·갱신 토큰을 즉시 차단한다. 새 비밀번호는 8자 이상·UTF-8 72바이트 이하다. GET /reset-password 화면에서도 처리할 수 있다. 만료·재사용·탈퇴 계정의 토큰은 거절하며 동시 요청은 한 번만 성공한다.

### 6.6 로그아웃과 회원 탈퇴

로그아웃은 JWT의 sid와 요청 본문 refreshToken의 세션·사용자 일치를 확인하고 해당 세션을 폐기한다. 이후 액세스 토큰도 즉시 거절하며 다른 기기의 세션은 유지한다. 다른 사용자의 갱신 토큰이나 다른 세션 토큰을 전달하면 401이다. 회원 탈퇴는 `confirmation: "DELETE"`를 요구하고 전체 세션 폐기·동의 철회·로컬 소셜 연결 삭제·이메일 익명화를 수행한다. 외부 Google/Kakao 측 연결 해제는 포함하지 않는다.

갱신 토큰은 사용 시 회전하고 같은 토큰의 동시 갱신은 한 번만 성공한다. 세션의 절대 수명은 최초 로그인부터 기본 30일이다. V5 배포 전 토큰에는 세션 정보가 없어 재로그인이 필요하다.

### 6.7 오늘 홈 요약

```http
GET /api/v1/home/today?date=2026-10-14
Authorization: Bearer {accessToken}
```

```json
{
  "data": {
    "date": "2026-10-14",
    "habitSummary": {
      "total": 3,
      "completed": 1,
      "remaining": 2
    },
    "mealSummary": {
      "recordedCount": 1,
      "caloriesKcal": 620
    },
    "nextMedication": {
      "occurrenceId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b1",
      "medicationName": "예시 약",
      "scheduledAt": "2026-10-14T13:00:00+09:00",
      "doseAmount": 1,
      "doseUnit": "TABLET"
    }
  },
  "traceId": "7b3470e0f0b142e7"
}
```

`nextMedication`이 없으면 `null`을 반환한다.

### 6.8 습관 생성

```http
POST /api/v1/habits
Authorization: Bearer {accessToken}
Idempotency-Key: {uuid}
```

```json
{
  "name": "물 마시기",
  "categoryCode": "HYDRATION",
  "startDate": "2026-10-14",
  "endDate": null,
  "priority": 2,
  "timezone": "Asia/Seoul",
  "schedule": {
    "daysOfWeek": [1, 2, 3, 4, 5, 6, 7],
    "times": ["09:00", "15:00"]
  }
}
```

```json
{
  "data": {
    "habitId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b2",
    "status": "ACTIVE"
  },
  "traceId": "7b3470e0f0b142e7"
}
```

요일은 ISO 기준 `1=월요일`, `7=일요일`을 사용한다.

### 6.9 오늘의 습관 회차

```http
GET /api/v1/habit-occurrences?date=2026-10-14
```

```json
{
  "data": {
    "items": [
      {
        "occurrenceId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b3",
        "habitId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b2",
        "habitName": "물 마시기",
        "scheduledAt": "2026-10-14T09:00:00+09:00",
        "status": "SCHEDULED",
        "reminderCount": 0
      }
    ]
  },
  "traceId": "7b3470e0f0b142e7"
}
```

### 6.10 습관 행동 기록

```http
POST /api/v1/habit-occurrences/{occurrenceId}/actions
Idempotency-Key: {uuid}
```

완료:

```json
{
  "action": "COMPLETE",
  "actualAt": "2026-10-14T09:08:00+09:00"
}
```

미루기:

```json
{
  "action": "SNOOZE",
  "snoozeMinutes": 15
}
```

건너뛰기:

```json
{
  "action": "SKIP",
  "reason": "오늘은 수행하지 않음"
}
```

응답은 서버가 확정한 최신 회차 상태와 다음 알림 예정 시각을 포함한다.

### 6.11 식사 생성

```http
POST /api/v1/meals
Authorization: Bearer {accessToken}
Idempotency-Key: {uuid}
```

```json
{
  "mealType": "LUNCH",
  "eatenAt": "2026-10-14T12:30:00+09:00",
  "status": "COMPLETED",
  "imageFileKey": null,
  "memo": "점심",
  "items": [
    {
      "foodId": null,
      "foodName": "닭가슴살",
      "amount": 150,
      "unitCode": "G",
      "nutrients": {
        "caloriesKcal": 165,
        "carbohydrateG": 0,
        "proteinG": 31,
        "fatG": 3.6,
        "sugarG": 0,
        "caffeineMg": 0
      }
    }
  ]
}
```

```json
{
  "data": {
    "mealId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b4",
    "eatenAt": "2026-10-14T12:30:00+09:00",
    "recordedAt": "2026-10-14T12:42:11+09:00",
    "totals": {
      "caloriesKcal": 165,
      "carbohydrateG": 0,
      "proteinG": 31,
      "fatG": 3.6,
      "sugarG": 0,
      "caffeineMg": 0
    },
    "rescheduledMedicationCount": 1
  },
  "traceId": "7b3470e0f0b142e7"
}
```

서버는 음식별 영양 스냅샷과 식사 총합을 저장하고, 관련 식후 복약 알림을 재계산한다.

### 6.12 의약품 검색

```http
GET /api/v1/drugs?keyword=타이레놀&page=0&size=20
```

```json
{
  "data": {
    "items": [
      {
        "drugId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b5",
        "name": "예시 의약품",
        "manufacturer": "예시 제약",
        "dosageForm": "정제",
        "imageUrl": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "traceId": "7b3470e0f0b142e7"
}
```

### 6.13 의약품 상세

```http
GET /api/v1/drugs/{drugId}
```

```json
{
  "data": {
    "drugId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b5",
    "name": "예시 의약품",
    "manufacturer": "예시 제약",
    "efficacy": "공공데이터에서 제공된 효능 정보",
    "usage": "공공데이터에서 제공된 사용법",
    "precautions": "공공데이터에서 제공된 주의사항",
    "sideEffects": "공공데이터에서 제공된 부작용 정보",
    "storage": "보관 정보",
    "appearance": {
      "dosageForm": "정제",
      "color": "흰색",
      "shape": "원형",
      "scoreLine": "없음"
    },
    "ingredients": [
      {
        "name": "예시 성분",
        "amount": 500,
        "unitCode": "MG"
      }
    ],
    "sourceName": "공공데이터 제공기관",
    "sourceUpdatedAt": "2026-10-01T00:00:00Z",
    "disclaimer": "이 정보는 진단이나 처방을 대신하지 않습니다."
  },
  "traceId": "7b3470e0f0b142e7"
}
```

### 6.14 개인 복약 생성

> 2026-10-17 1차 구현: 기본정보 등록·조회·수정·중지·종료만 지원합니다.
> 아래 schedule 포함 예시는 후속 목표입니다. 현재 schedule 입력은 400으로 거절하며 일정·회차를 생성하지 않습니다.
> 실제 요청/응답·version·Idempotency-Key·지원 유형은 backend/PERSONAL_MEDICATION_API.md를 기준으로 연결합니다.

```http
POST /api/v1/medications
Authorization: Bearer {accessToken}
Idempotency-Key: {uuid}
```

```json
{
  "drugId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b5",
  "customName": null,
  "medicationType": "OTC",
  "purpose": "예시 목적",
  "startDate": "2026-10-14",
  "endDate": "2026-10-20",
  "schedule": {
    "daysOfWeek": [1, 2, 3, 4, 5, 6, 7],
    "times": [
      {
        "sequence": 1,
        "fixedTime": null,
        "mealRelation": "AFTER_MEAL",
        "mealType": "LUNCH",
        "offsetMinutes": 30,
        "doseAmount": 1,
        "doseUnitCode": "TABLET"
      }
    ]
  }
}
```

`drugId`와 `customName` 중 하나는 존재해야 한다. 직접 입력 영양제·보충제는 `customName`을 사용한다.

```json
{
  "data": {
    "medicationId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b6",
    "status": "ACTIVE"
  },
  "traceId": "7b3470e0f0b142e7"
}
```

### 6.15 오늘의 복약 회차

```http
GET /api/v1/medication-occurrences?date=2026-10-14
```

```json
{
  "data": {
    "items": [
      {
        "occurrenceId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b7",
        "medicationId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b6",
        "medicationName": "예시 의약품",
        "scheduledAt": "2026-10-14T13:00:00+09:00",
        "mealRelation": "AFTER_MEAL",
        "doseAmount": 1,
        "doseUnitCode": "TABLET",
        "status": "SCHEDULED"
      }
    ]
  },
  "traceId": "7b3470e0f0b142e7"
}
```

### 6.16 실제 복약 행동

```http
POST /api/v1/medication-occurrences/{occurrenceId}/actions
Idempotency-Key: {uuid}
```

복용:

```json
{
  "action": "TAKE",
  "actualAt": "2026-10-14T13:07:00+09:00",
  "actualDoseAmount": 1,
  "doseUnitCode": "TABLET",
  "memo": null
}
```

건너뜀:

```json
{
  "action": "SKIP",
  "memo": "오늘은 복용하지 않음"
}
```

약 없음:

```json
{
  "action": "NO_STOCK",
  "memo": "재구매 필요"
}
```

서버는 실제 행동 시각과 요청을 기록한 서버 시각을 구분하고, 남은 재알림 작업을 갱신한다.

### 6.17 알림 설정

```http
PUT /api/v1/notification-settings
```

```json
{
  "enabled": true,
  "reminderEnabled": true,
  "reminderIntervalMinutes": 15,
  "maxReminderCount": 2,
  "quietHours": {
    "enabled": true,
    "startTime": "22:00",
    "endTime": "07:00"
  }
}
```

허용 범위와 기본값은 알림 정책서에서 확정한다.

### 6.18 기본 통계

```http
GET /api/v1/statistics/summary?from=2026-10-08&to=2026-10-14
```

```json
{
  "data": {
    "period": {
      "from": "2026-10-08",
      "to": "2026-10-14"
    },
    "habits": {
      "scheduled": 14,
      "completed": 10,
      "skipped": 1,
      "completionRate": 71.43
    },
    "meals": {
      "recordedCount": 12,
      "totals": {
        "caloriesKcal": 12600,
        "carbohydrateG": 1400,
        "proteinG": 620,
        "fatG": 410,
        "sugarG": 230,
        "caffeineMg": 700
      }
    },
    "medications": {
      "scheduled": 14,
      "taken": 12,
      "skipped": 1,
      "adherenceRate": 85.71
    }
  },
  "traceId": "7b3470e0f0b142e7"
}
```

### 6.19 분석 이벤트 수신

```http
POST /api/v1/analytics/events
```

```json
{
  "events": [
    {
      "eventId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b8",
      "occurredAt": "2026-10-14T13:07:03+09:00",
      "sessionId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b9",
      "eventName": "medication_intake_completed",
      "screenName": "MED-01",
      "properties": {
        "occurrenceId": "0193c0d1-9b11-7d42-9b3d-b7f9ccac82b7",
        "entrySource": "notification"
      },
      "schemaVersion": 1
    }
  ]
}
```

자유입력 메모, 약 이름, 증상 원문 등 민감정보는 분석 이벤트 속성에 포함하지 않는다.

## 7. 정렬·페이지네이션 규칙

- `page`는 0부터 시작한다.
- 기본 `size`는 20, 최대값은 100으로 제한한다.
- 허용된 정렬 필드만 사용한다.
- 동일 정렬값에서는 ID 등 안정적인 보조 정렬을 사용한다.
- 무제한 전체 목록 API를 만들지 않는다.

## 8. 소유권과 보안 규칙

- 사용자 소유 리소스는 토큰의 사용자 ID를 기준으로 조회한다.
- 클라이언트가 요청 본문에 임의의 `userId`를 보내지 않는다.
- URL의 리소스 ID가 다른 사용자 소유라면 존재 여부를 과도하게 노출하지 않는다.
- 의약품 공통 사전과 사용자 복약 데이터를 분리한다.
- 파일 업로드는 허용 MIME, 확장자, 크기와 악성 파일 검사를 적용한다.
- API 로그에는 Authorization 헤더와 민감정보 원문을 남기지 않는다.

## 9. 멱등성과 중복 방지

다음 요청은 `Idempotency-Key` 적용을 권장한다.

- 회원가입
- 습관 생성
- 습관 완료·미루기·건너뛰기
- 식사 생성
- 개인 복약 생성
- 실제 복약 행동
- 분석 이벤트 묶음 수신

동일 사용자·동일 키·동일 요청은 최초 성공 결과를 반환한다. 같은 키로 다른 요청 본문이 들어오면 `IDEMPOTENCY_CONFLICT`를 반환한다.

## 10. OpenAPI 파일 분리 권장안

```text
docs/api/
├─ openapi.yaml
├─ paths/
│  ├─ auth.yaml
│  ├─ habits.yaml
│  ├─ meals.yaml
│  ├─ drugs.yaml
│  ├─ medications.yaml
│  ├─ notifications.yaml
│  └─ statistics.yaml
└─ schemas/
   ├─ common.yaml
   ├─ auth.yaml
   ├─ habit.yaml
   ├─ meal.yaml
   ├─ medication.yaml
   └─ statistics.yaml
```

MVP 초기에는 단일 `openapi.yaml`로 시작하고 파일이 커질 때만 분리한다.

## 11. API 변경 절차

1. 변경 이유와 영향 화면을 확인한다.
2. OpenAPI 명세를 먼저 수정한다.
3. 성준과 현서가 요청·응답 예제를 함께 확인한다.
4. 백엔드 구현과 테스트를 수정한다.
5. Android Mock과 실제 API 모델을 수정한다.
6. 통합 테스트 후 PR에 변경 내용을 기록한다.

호환성을 깨는 변경은 기존 필드를 즉시 제거하기보다 새 필드나 새 API 버전을 검토한다.

## 12. 확정이 필요한 API 정책

- JWT 또는 다른 토큰 방식
- Refresh Token 저장·폐기 정책
- 공개 의약품 검색 허용 여부
- 파일 업로드 저장소와 업로드 방식
- 사용자 삭제와 기록 보존 정책
- 재알림 간격·최대 횟수의 허용 범위
- 식사 기록이 없을 때 식후 복약 처리
- 리소스 삭제를 논리 삭제로 통일할지 여부
- API Rate Limit
- 앱 최소 지원 버전과 강제 업데이트 정책

이 항목이 확정되면 v0.2에서 OpenAPI YAML과 함께 갱신한다.

