# 인증 기능 실행·연동 안내

## 현재 구현 범위

이메일 가입·인증·재발송, 이메일 로그인, Google/Kakao 토큰 기반 로그인, 갱신, 로그아웃, 비밀번호 재설정, 회원 탈퇴를 제공한다. 이메일 링크에서 사용할 최소 인증·재설정 웹 화면도 백엔드에 포함했다.

## 실행

1. Java 21과 MariaDB 11.4를 준비한다.
2. backend 폴더에서 DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET을 환경변수로 지정한다.
3. Maven Wrapper로 실행한다: `./mvnw.cmd spring-boot:run`.
4. 개발용 도구가 `.tools/java21`에 있다면 해당 JDK 디렉터리를 JAVA_HOME으로 설정한다.
5. 최초 실행 시 Flyway V1~V7를 적용한다. V5 업데이트 이전에 로그인한 사용자는 재로그인이 필요하다. 프로필·온보딩·동의 변경은 PROFILE_ONBOARDING_API.md를 참고한다.

`.env.example`는 변수 목록이다. Maven 실행은 .env를 자동으로 불러오지 않는다.

## 이메일 인증 시연

1. POST /api/v1/auth/signup에 email, password, 필수 동의 3종을 전송한다.
2. 가입 응답 상태는 PENDING_EMAIL_VERIFICATION이다.
3. 개발 기본값 MAIL_MODE=console에서는 서버 로그에 출력된 /verify-email#token=... 링크를 연다.
4. 이메일 인증하기 버튼을 누른다. GET으로 화면만 열었을 때는 인증 토큰을 사용하지 않는다.
5. 앱 또는 POST /api/v1/auth/login에서 이메일과 비밀번호로 로그인한다.
6. 응답의 accessToken을 Authorization: Bearer 헤더에 사용한다.

## 비밀번호 재설정 시연

1. POST /api/v1/auth/password-resets/request에 email을 전송한다.
2. /reset-password#token=... 링크를 연다.
3. 새 비밀번호와 확인값을 입력한다.
4. 완료하면 기존 모든 세션의 액세스·갱신 토큰이 차단된다.
5. 새 비밀번호로 다시 로그인한다.

비밀번호는 8자 이상, UTF-8 기준 72바이트 이하이다. 기본 인증·재설정 토큰 수명은 30분이고, 같은 종류의 메일은 계정별 60초 간격이다. 요청 응답은 계정 존재 여부·쿨다운 여부와 무관하게 accepted=true이며, 실제 발송이 일어났다는 뜻은 아니다. 새 링크를 발급하면 이전 미사용 링크를 무효화한다.

## 실제 메일 연결

- MAIL_MODE=smtp
- MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM 설정
- SMTP 서비스에 맞게 MAIL_SMTP_AUTH, MAIL_STARTTLS 설정
- EMAIL_VERIFICATION_BASE_URL=https://서비스주소/verify-email
- PASSWORD_RESET_BASE_URL=https://서비스주소/reset-password

실제 비밀값은 환경변수에만 설정하고 Git·노션·채팅에 붙이지 않는다. 로컬 SMTP 전송 테스트는 통과했지만 외부 SMTP 인증·메일 수신함 도착·스팸 분류는 해당 계정으로 확인해야 한다.

## Google 연결

GOOGLE_CLIENT_ID를 백엔드가 수락할 OAuth 클라이언트 ID로 지정한다. Android에서 이 대상 클라이언트에 대해 발급받은 ID 토큰을 아래 API로 보낸다.

```json
{
  "provider": "GOOGLE",
  "token": "앱에서 발급받은 ID 토큰",
  "consents": [
    {"type": "TERMS_OF_SERVICE", "version": "2026-10", "agreed": true},
    {"type": "PRIVACY", "version": "2026-10", "agreed": true},
    {"type": "SENSITIVE_HEALTH_DATA", "version": "2026-10", "agreed": true}
  ]
}
```

경로는 POST /api/v1/auth/social이다. 기존 소셜 회원은 동의를 다시 전송하지 않아도 된다. 이 API는 공급자 로그인 화면이나 인가 코드 교환을 대신하지 않는다.

공식 기준: [Google 백엔드 인증](https://developers.google.com/identity/sign-in/android/backend-auth)

## Kakao 연결

KAKAO_APP_ID에 개발자 콘솔의 숫자 앱 ID를 지정한다. REST API 키나 Admin 키가 아니다. 앱 SDK에서 발급받은 사용자 액세스 토큰을 provider=KAKAO로 같은 API에 보낸다.

백엔드는 /v1/user/access_token_info에서 앱 ID·만료를 확인하고 /v2/user/me의 사용자 ID와 일치하는지 검사한다. 검증되지 않은 이메일은 계정 이메일로 신뢰하지 않는다. 타임아웃·5xx 등은 공급자 연결 오류로 처리한다.

공식 기준: [Kakao 로그인 REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api)

## 클라이언트 토큰 처리

- 액세스 토큰은 기본 1시간이며 각 요청에서 로그인 세션 상태를 함께 확인한다.
- 갱신 요청은 클라이언트에서 한 번만 진행하도록 묶는다. 동시 갱신 시 하나만 성공하며 나머지는 INVALID_REFRESH_TOKEN이다.
- 갱신 성공 후 새 액세스·갱신 토큰 쌍을 함께 저장한다.
- 로그아웃은 현재 JWT와 같은 세션에 속한 refreshToken을 전송한다. 해당 기기만 차단한다.
- 비밀번호 재설정과 탈퇴는 모든 기기를 차단한다.
- 세션의 절대 수명은 기본 30일이며 갱신해도 연장하지 않는다.
- 401이면 저장된 토큰 상태를 정리하고 로그인 흐름으로 이동한다.
- 웹 인증 화면은 토큰을 로컬 저장소에 저장하지 않는다.

## 현재 남은 범위

- 서비스의 실제 Google/Kakao 앱 설정값과 실사용 계정을 이용한 모바일 E2E 검증.
- 실제 SMTP 계정의 수신함 도착 검증.
- Android 로그인 화면·SDK·토큰 보관 및 인증 링크 앱 복귀 연동.
- 서로 다른 로그인 계정을 연결하는 별도 API와 공급자 측 연결 해제는 미구현. 이메일이 같아도 자동 병합하지 않고 기존 로그인 방식을 사용하게 한다.
- 로그인 시도 전반의 IP/계정별 속도 제한은 아직 없으며, 현재 구현한 제한은 인증·재설정 메일의 계정별 발송 간격이다.

## 검증 명령

기본 H2 검증: `./mvnw.cmd test`

실제 MariaDB 검증: `./scripts/Test-MariaDb.ps1`. 프로젝트 .tools에 도구가 없으면 MariaDbHome 및 JavaHome 매개변수를 지정한다. 스크립트는 새 임시 데이터 디렉터리·임의 로컬 포트만 사용하고 테스트가 끝나면 서버를 종료한다.

테스트 결과·환경·확인되지 않은 범위는 DEVELOPMENT_LOG.md 및 연결된 노션 문서를 기준으로 확인한다.
