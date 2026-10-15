# 루티브 AI 건강습관 백엔드

성준 담당 Spring Boot REST API 프로젝트입니다. 현재 인증·회원 기반과 의약품 사전 조회를 구현했습니다.

의약품 사전의 검색·정렬·오류 응답과 검증 범위는 [DRUG_DICTIONARY_API.md](DRUG_DICTIONARY_API.md)를 참고합니다.

## 기술 기준

- Java 21 LTS
- Spring Boot 4.0.8
- Maven Wrapper
- Spring MVC, Spring Data JPA, Spring Security
- MariaDB 11.4 LTS
- Flyway

## 현재 구현 범위

- `GET /api/v1/health`: 서버 상태 확인
- `POST /api/v1/auth/signup`: 필수 동의와 함께 회원가입
- `POST /api/v1/auth/email-verifications/confirm`: 이메일 인증 완료 및 로그인 토큰 발급
- `POST /api/v1/auth/email-verifications/resend`: 이메일 인증 메일 재발송
- `POST /api/v1/auth/login`: 이메일·비밀번호 로그인
- `POST /api/v1/auth/social`: Google ID 토큰 또는 Kakao 액세스 토큰으로 로그인·가입
- `POST /api/v1/auth/refresh`: 갱신 토큰 회전 및 토큰 재발급
- `POST /api/v1/auth/password-resets/request`: 비밀번호 재설정 메일 요청
- `POST /api/v1/auth/password-resets/confirm`: 비밀번호 재설정 완료
- `POST /api/v1/auth/logout`: 현재 갱신 토큰 폐기
- `GET /api/v1/users/me`: JWT 인증 사용자와 동의 내역 조회
- `PATCH /api/v1/users/me`: 닉네임·생년월일·성별·키 부분 수정
- `POST /api/v1/users/me/onboarding/complete`: 기본 프로필 온보딩 완료
- `GET /api/v1/users/me/consents`, `POST /api/v1/users/me/consents`: 동의 이력 조회·철회·재동의
- `DELETE /api/v1/users/me`: 회원 탈퇴 및 로그인 수단 폐기
- `GET /api/v1/drugs?keyword=...&page=0&size=20`: 의약품명·성분·업체 검색
- `GET /api/v1/drugs/{drugId}`: 의약품 상세 조회
- 공통 성공·오류 응답과 `X-Trace-Id`
- MariaDB 로컬 실행 구성
- 의약품 사전 Flyway 마이그레이션과 시연 데이터

의약품 사전은 인증 없이 조회할 수 있습니다. 사용자 API에는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

이메일 회원가입 계정은 `PENDING_EMAIL_VERIFICATION` 상태로 생성되며 이메일 인증을 완료해야 로그인할 수 있습니다. 비밀번호는 BCrypt로 해시하고, 인증·재설정·갱신 토큰은 원문을 저장하지 않고 해시만 보관합니다. 갱신 토큰은 사용할 때마다 회전됩니다.

JWT의 `sid`와 DB 로그인 세션을 함께 검증합니다. 로그아웃은 현재 기기의 세션을 폐기하여 액세스·갱신 토큰을 즉시 차단하며 다른 기기의 세션은 유지합니다. 비밀번호 재설정과 탈퇴는 모든 세션을 폐기합니다. 세션 수명은 최초 로그인부터 기본 30일이며 갱신으로 무한 연장하지 않습니다.

V5 마이그레이션은 세션 정보가 없는 기존 갱신 토큰을 폐기합니다. 업데이트 후 기존 사용자는 한 번 다시 로그인해야 합니다.

V6는 온보딩 완료 시각과 재동의 이력 저장을 추가합니다. 프로필 입력과 동의 변경의 요청·응답·제한은 [PROFILE_ONBOARDING_API.md](PROFILE_ONBOARDING_API.md)를 참고합니다. 온보딩은 기본 프로필까지이며 생활패턴 설문은 후속 범위입니다.

## 사전 준비

1. JDK 21 설치
2. Docker Desktop 설치 및 실행

Maven은 별도 설치하지 않아도 프로젝트의 Maven Wrapper를 사용합니다.

## 로컬 실행

PowerShell에서 `backend` 폴더로 이동한 뒤 실행합니다.

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

서버 확인:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
Invoke-RestMethod 'http://localhost:8080/api/v1/drugs?keyword=예시&page=0&size=20'
Invoke-RestMethod http://localhost:8080/api/v1/drugs/0193c0d1-9b11-7d42-9b3d-b7f9ccac82b5
```

회원가입 예시:

```powershell
$body = @{
  email = 'user@example.com'
  password = 'strong-password-1234'
  consents = @(
    @{ type = 'TERMS_OF_SERVICE'; version = '2026-10'; agreed = $true }
    @{ type = 'PRIVACY'; version = '2026-10'; agreed = $true }
    @{ type = 'SENSITIVE_HEALTH_DATA'; version = '2026-10'; agreed = $true }
  )
} | ConvertTo-Json -Depth 4

$result = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/auth/signup `
  -ContentType 'application/json' `
  -Body $body
```

회원가입 응답에는 로그인 토큰 대신 `PENDING_EMAIL_VERIFICATION` 상태가 반환됩니다. 로컬 기본 설정에서는 인증 링크가 서버 로그에 출력되며, 운영 환경에서는 아래 SMTP 설정을 사용해야 합니다.

소셜 로그인 요청 예시:

```json
{
  "provider": "GOOGLE",
  "token": "google-id-token",
  "consents": [
    { "type": "TERMS_OF_SERVICE", "version": "2026-10", "agreed": true },
    { "type": "PRIVACY", "version": "2026-10", "agreed": true },
    { "type": "SENSITIVE_HEALTH_DATA", "version": "2026-10", "agreed": true }
  ]
}
```

`provider`는 `GOOGLE` 또는 `KAKAO`입니다. Google은 앱에서 받은 ID 토큰을, Kakao는 사용자 액세스 토큰을 `token`에 전달합니다. 같은 이메일의 기존 계정은 자동 연결하지 않으며 `ACCOUNT_LINK_REQUIRED` 오류와 함께 기존 로그인 방식 사용을 안내합니다. 별도의 소셜 계정 연결 API는 현재 범위에 포함하지 않습니다.

Google은 서명·발급자·대상 클라이언트·만료·발급 시각·사용자 ID·인증된 이메일을 검사합니다. Kakao는 토큰 정보 API에서 앱 ID·만료를 먼저 확인하고 사용자 정보의 ID와 대조합니다. `KAKAO_APP_ID`는 REST API 키가 아니라 개발자 콘솔의 숫자 앱 ID입니다.

## 이메일 링크 화면

- `GET /verify-email`: 사용자가 버튼을 눌러 이메일 인증을 완료하는 화면
- `GET /reset-password`: 새 비밀번호·확인값을 입력하는 화면
- 링크 형식: `/verify-email#token=...`, `/reset-password#token=...`

링크를 열기만 해서는 토큰을 소비하지 않습니다. 화면은 토큰을 저장소에 남기지 않으며 URL에서 제거한 후 POST 요청으로 제출합니다. 완료 후 앱에서 로그인합니다. 기존 query 방식 링크도 읽을 수 있습니다.

인증·재설정 메일은 계정별 기본 60초 간격으로 제한하며 제한 중 요청은 동일한 accepted 응답만 반환합니다. 새 링크가 발급되면 이전 미사용 링크는 무효화합니다. 비밀번호는 8자 이상·UTF-8 72바이트 이하입니다.

## 테스트

```powershell
.\mvnw.cmd test
```

실제 MariaDB 검증은 별도의 빈 테스트 DB에서만 실행합니다. 테스트가 계정 데이터를 정리하므로 운영 DB를 지정하면 안 됩니다.

```powershell
.\scripts\Test-MariaDb.ps1 -MariaDbHome 'C:\tools\mariadb-11.4.8-winx64' -JavaHome 'C:\tools\jdk-21'
```

도구가 `.tools/mariadb`, `.tools/java21`에 있으면 인수를 생략할 수 있습니다. 이 스크립트는 로컬 전용 임시 MariaDB를 시작하고 V1~V6 및 JPA 스키마 검증, 전체 테스트를 실행한 후 서버를 종료합니다. 테스트 데이터는 `.tools/db-test-*`에 남습니다. `.tools`는 Git에서 제외됩니다.

테스트 구성과 최근 결과는 [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md)에 기록합니다. Google은 로컬 RSA 서명, Kakao는 HTTP 모의 응답, SMTP는 실제 루프백 소켓을 사용합니다. 실제 공급자 로그인·외부 수신함 도착·모바일 화면 연동은 별도 검증이 필요합니다.

## 환경변수

| 이름 | 기본값 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:mariadb://localhost:3306/health_habit` | MariaDB JDBC URL |
| `DB_USERNAME` | `health_habit` | DB 사용자 |
| `DB_PASSWORD` | `health_habit_local` | 로컬 DB 비밀번호 |
| `SERVER_PORT` | `8080` | HTTP 포트 |
| `GOOGLE_CLIENT_ID` | 없음 | Google 로그인용 OAuth 클라이언트 ID |
| `KAKAO_APP_ID` | 없음 | Kakao 숫자 앱 ID, 다른 앱의 토큰 차단에 사용 |
| `MAIL_COOLDOWN_SECONDS` | `60` | 계정별 인증·재설정 메일 재발송 간격 |
| `MAIL_MODE` | `console` | 로컬 로그 출력은 `console`, 실제 발송은 `smtp` |
| `MAIL_FROM` | 로컬 주소 | 발신 이메일 주소 |
| `MAIL_HOST`, `MAIL_PORT` | 없음, `587` | SMTP 서버와 포트 |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | 없음 | SMTP 인증 정보 |
| `EMAIL_VERIFICATION_BASE_URL` | 로컬 URL | 이메일 인증 화면 URL |
| `PASSWORD_RESET_BASE_URL` | 로컬 URL | 비밀번호 재설정 화면 URL |

운영 환경에서는 기본 비밀번호를 사용하지 않습니다. `.env`와 비밀값은 Git에 커밋하지 않습니다.

Spring Boot의 Maven 실행은 `.env` 파일을 자동으로 읽지 않습니다. PowerShell 환경변수 또는 IDE 실행 설정에 값을 넣어야 합니다. 실제 발송 시 MAIL_MODE=smtp, SMTP 계정, 외부에서 접근 가능한 HTTPS 인증·재설정 URL을 지정합니다.

## 패키지 구조

```text
com.routive.healthhabit
├─ common        공통 응답, 오류, 추적 ID
├─ config        보안 등 애플리케이션 설정
├─ health        서버 상태 확인
├─ auth          로컬·소셜 인증, 토큰, 이메일 발송
├─ user          사용자 조회와 탈퇴
└─ drug
   ├─ api        Controller와 응답 DTO
   ├─ application 업무 흐름
   └─ domain     Entity와 Repository
```

## 다음 구현 순서

1. 공공 의약품 데이터 출처 확정·적재·갱신 (사전 전용 테스트 완료)
2. 사전 상세에서 개인 복약 등록
3. 복약 일정과 실제 복용 기록
4. 습관, 식사, 알림 순으로 확장

앱 개발과 함께 프로필·온보딩 API 및 Google/Kakao SDK의 실제 연결을 검증합니다. 생활패턴 설문, 약관 게시 버전 관리와 동의 철회 시 기존 데이터 처리 정책은 후속 범위입니다.
