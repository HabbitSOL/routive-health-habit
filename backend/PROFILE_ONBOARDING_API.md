# 프로필·온보딩·동의 API

구현 시작: 2026-10-14, 검증·문서 마무리: 2026-10-15. 성준 백엔드. 모든 경로는 /api/v1 접두어와 Bearer 인증이 필요하다.
화면 연결은 현서 담당이며 현재 서버 기능만 구현했다.

## 앱 연결 순서

1. 이메일 인증 또는 소셜 로그인으로 토큰을 발급받는다.
2. GET /users/me에서 계정, 동의 이력, profile을 확인한다.
3. 필수 동의가 유효한 상태에서 PATCH /users/me로 프로필을 저장한다.
4. POST /users/me/onboarding/complete를 호출한다. 본문은 필요 없다.
5. profile.onboardingCompletedAt이 null이 아니면 최초 온보딩을 완료한 사용자다.

완료 시각은 최초 완료의 이력이다. 현재 필수 동의가 철회되었는지는 consents에서 별도로 판단한다.
현재 온보딩은 ONB-02 기본 프로필 범위다. 운동 빈도·수면·식사 패턴 설문과 관심 카테고리는 아직 구현하지 않았다.

## 프로필 조회·수정

GET /users/me는 기존 계정 응답을 유지하면서 profile 객체를 추가한다.
미등록 사용자는 profile 내부 값이 모두 미설정이다. 운영의 non_null 직렬화 설정에서는 null 필드가 생략되어 빈 객체가 될 수 있다. 앱에서는 누락과 null을 모두 미설정으로 처리한다. 조회만으로 DB 행을 만들지 않는다.

PATCH /users/me 예시:

```json
{
  "nickname": "성준",
  "birthDate": "2000-01-01",
  "sex": "MALE",
  "heightCm": 175.25
}
```

응답 data는 nickname, birthDate, sex, heightCm, onboardingCompletedAt을 가진 프로필 객체다.

| 필드 | 규칙 |
| --- | --- |
| nickname | 선택 수정. 입력 시 공백만 불가, 최대 30자, 양끝 공백 제거. 온보딩 완료에는 필수 |
| birthDate | YYYY-MM-DD, 미래 날짜 불가. 선택 |
| sex | MALE, FEMALE, OTHER, UNSPECIFIED 중 하나. 선택 |
| heightCm | 1~300, 소수 둘째 자리까지. 선택. 서비스 입력 범위이며 의학적 정상 범위 판정이 아님 |
| clearFields | birthDate, sex, heightCm 중 지울 필드 이름 배열. 같은 요청에서 해당 값을 함께 주면 400 |

생략하거나 null로 보낸 필드는 기존 값을 유지한다. 선택값 삭제는 아래처럼 명시한다.

```json
{"clearFields":["birthDate","heightCm"]}
```

닉네임 삭제는 지원하지 않는다. 이메일·사용자 ID·역할·계정 상태는 이 API로 변경할 수 없다.
몸무게는 별도 신체측정 이력으로 설계하며 이번 API에 포함하지 않는다.
본인 계정은 JWT subject에서 결정한다. 클라이언트가 대상 사용자 ID를 선택하지 않는다.
회원 탈퇴 시 프로필과 온보딩 완료 시각은 삭제한다.

## 온보딩 완료

POST /users/me/onboarding/complete

- 닉네임과 유효한 필수 동의 3종이 필요하다.
- 필수 동의: TERMS_OF_SERVICE, PRIVACY, SENSITIVE_HEALTH_DATA.
- 조건 미충족은 400. 완료 재요청은 200이며 최초 완료 시각을 유지한다.
- 생년월일·성별·키는 입력하지 않아도 완료할 수 있다.

## 동의 조회·변경

GET /users/me/consents는 최신 동의 시각 순으로 전체 이력을 반환한다.
각 항목은 type, version, consentedAt, withdrawnAt이다.
withdrawnAt이 null이거나 응답에서 생략되어 있으면 현재 유효한 동의다. 미동의 항목은 행이 없을 수 있다.

POST /users/me/consents는 한 종류씩 변경한다.

```json
{"type":"SENSITIVE_HEALTH_DATA","version":"2026-10","agreed":false}
```

- 지원 종류: 필수 3종 및 선택 MARKETING. agreed는 반드시 true/false를 지정한다.
- 같은 유효 버전 재동의·이미 철회된 항목 재철회는 중복 이력을 생성하지 않는다.
- 철회 후 같은 버전 재동의는 새 행을 만들고 기존 동의/철회 시각을 보존한다.
- 다른 버전에 동의하면 이전 유효 동의를 종료하고 새 동의를 기록한다.
- 현재 유효 버전과 다른 버전의 철회 요청은 409 DATA_CONFLICT. 새로 조회 후 사용자에게 확인한다.
- 사용자 행 잠금으로 변경 요청을 직렬화한다.
- 필수 동의 철회 후 프로필 수정과 온보딩 완료는 400 REQUIRED_CONSENT_MISSING.
- 조회·재동의·로그아웃·탈퇴는 가능하다. 철회 자체가 탈퇴나 기존 프로필 즉시 삭제를 뜻하지 않는다.

## 제한 및 후속 작업

- 약관 원문/게시 버전 레지스트리와 현재 유효 버전 강제 검증은 아직 없다. 버전 문자열은 기존 가입 계약처럼 클라이언트에서 전달한다. 운영 전 약관 게시·버전 검증 정책을 연결해야 한다.
- 동의 철회에 따른 기존 데이터 보존·삭제 정책은 별도 확정이 필요하다. 본 구현은 법적 적합성 인증이 아니다.
- 향후 식사·복약·습관 저장 및 분석 처리에도 동의 검사를 연결해야 한다. 현재 미구현 기능까지 차단을 보장하지 않는다.
- 관심 카테고리/생활패턴 설문, 시간대/언어 수정, 신체측정 기록은 후속 기능이다.
- 실제 Android 화면·통신·사용자 조작 E2E는 아직 확인하지 않았다.

## DB

V6__profile_onboarding_and_consent_history.sql:

- user_profiles.onboarding_completed_at 추가.
- user_consents의 버전별 유니크 제약을 일반 조회 인덱스로 교체하여 재동의 이력 보존.
- 새 테이블 없음: 실행용 테이블 11개 유지. 기존 V1~V5는 변경하지 않음.
- 프로필 저장·동의 변경·탈퇴는 같은 사용자 행 잠금을 사용한다.

## 검증 결과

- H2: 2026-10-14 20:33:37 KST에 전체 59개 통과. JPA create-drop, Flyway 비활성.
- MariaDB 11.4.8: 2026-10-15 01:10:47 KST에 전체 59개 통과. Flyway V1~V6 및 JPA validate 성공.
- 두 환경 모두 실패·오류·건너뜀 0. 상세 결과는 DEVELOPMENT_LOG.md에 보존한다.
- 이번 범위의 통합 테스트 10개: 부분 수정·선택값 삭제, 잘못된 입력과 저장 롤백, 온보딩 조건·반복 요청, 동의 철회·재동의 이력, 이전 버전 철회 충돌, 잘못된 동의 요청, 미인증 접근, 탈퇴 시 프로필 삭제, 동시 동의, 사용자 간 격리.
