# 백엔드 개발 현황 및 테스트 기록

노션 문서: https://app.notion.com/p/3f99280f6a7b817886c2e04f119a149c

> **프로필·기본 온보딩·동의 변경 구현 및 검증 완료**
> 기준일: 2026-10-15 · 담당: 성준 백엔드.
> 최종 테스트: H2 59개 통과(2026-10-14), 실제 MariaDB 11.4.8 59개 통과(2026-10-15). 실패·오류·건너뜀 0. Flyway V1~V6 및 JPA validate 성공. 실제 외부 계정·Android 앱 연결은 별도 검증 대상.


## 1. 현재 개발 현황
| 영역 | 구현 상태 | 검증 및 남은 범위 |
| --- | --- | --- |
| 기반·사전 | Java 21, Spring Boot 4.0.8, 공통 응답, 헬스, 의약품 검색·상세, 시연 데이터 | 헬스·기동 검증. 의약품 검색/상세 전용 자동 테스트·공공데이터 적재는 남음 |
| 회원가입·동의 | 이메일 정규화·중복 방지·필수 동의·중복 동의 검증·BCrypt | 가입→인증→내 정보 조회 통과. 비밀번호 UTF-8 72바이트 제한 |
| 이메일 인증 | 대기 상태, 인증·재발송·만료·일회용, 60초 발송 간격 | 만료·재사용·재발송 무효화·동시 제출 테스트 통과 |
| 로그인·세션 | JWT sid와 DB 세션 검증, 갱신 토큰 회전, 30일 절대 세션 수명 | 정상 회전·이전 토큰 거절·동시 갱신·세션 만료 테스트 통과 |
| 소셜 로그인 | Google 서명·클레임 검증, Kakao 앱 ID·만료·사용자 ID 검증, 기존 계정 재로그인 | 로컬 RSA 및 HTTP 모의 응답 테스트. 실제 Google/Kakao 계정 로그인은 미검증 |
| 비밀번호 재설정 | 새 링크로 이전 링크 무효화, 일회용·만료 검증, 전체 세션 폐기 | 기존 비밀번호·액세스·갱신 거절, 동시 재설정 1회 성공 |
| 로그아웃 | 현재 세션 즉시 폐기, 다른 기기 유지, 사용자·세션 일치 확인 | 현재 액세스 즉시 거절·다른 기기 유지·타 사용자 토큰 거절 검증 |
| 회원 탈퇴 | DELETED, 이메일 익명화, 비밀번호 삭제, 동의 철회, 로컬 소셜 삭제, 전체 세션 폐기 | 상태·DB 변경 및 접근·갱신·재설정 차단 검증 |
| 메일·링크 화면 | SMTP/콘솔 발송, /verify-email 및 /reset-password 화면 | 로컬 SMTP 소켓으로 실제 메일 전송·본문 확인, 공개 라우팅 검증. 브라우저/앱 E2E는 별도 |
| 프로필·기본 온보딩 | 닉네임·생년월일·성별·키 부분 수정, 선택값 삭제, 최초 완료 시각, 탈퇴 시 프로필 삭제 | 입력 검증·반복 완료·사용자 격리 검증. 생활패턴 설문·신체측정은 후속 |
| 동의 변경 | 동의 이력 조회, 철회·재동의, 버전 변경 이력 보존, 동시 요청 직렬화 | 필수 동의 철회 시 프로필 저장·온보딩 차단. 약관 게시 버전 검증 및 기존 데이터 보존·삭제 정책은 후속 |
| 후속 제품 기능 | 개인 복약·식사·습관·알림·통계는 아직 미구현 | 다음 제품 개발은 사전 검증·공공데이터 적재·개인 복약 등록 |
## 2. 구현된 API
공통 접두어는 `/api/v1`입니다.
| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| GET | /health | 서버 상태 |
| GET | /drugs | 의약품 검색 |
| GET | /drugs/\{drugId\} | 의약품 상세 |
| POST | /auth/signup | 이메일 가입·인증 메일 요청 |
| POST | /auth/login | 이메일 로그인 |
| POST | /auth/social | GOOGLE 또는 KAKAO 로그인·신규 가입 |
| POST | /auth/refresh | 갱신 토큰 회전 |
| POST | /auth/email-verifications/confirm | 이메일 인증·토큰 발급 |
| POST | /auth/email-verifications/resend | 인증 메일 재발송 |
| POST | /auth/password-resets/request | 재설정 메일 요청 |
| POST | /auth/password-resets/confirm | 새 비밀번호 저장 |
| POST | /auth/logout | 인증 필요·현재 세션 폐기 |
| GET | /users/me | 인증 필요·내 계정과 동의 조회 |
| DELETE | /users/me | 인증 필요·confirmation 값 DELETE로 탈퇴 |
| PATCH | /users/me | 인증 필요·프로필 부분 수정 |
| POST | /users/me/onboarding/complete | 인증 필요·최초 기본 온보딩 완료 |
| GET | /users/me/consents | 인증 필요·전체 동의 이력 조회 |
| POST | /users/me/consents | 인증 필요·종류별 동의·철회·재동의 |

프로필·동의 API의 요청/응답, 입력 범위, 선택값 삭제 및 미구현 정책은 [PROFILE_ONBOARDING_API.md](PROFILE_ONBOARDING_API.md)를 참고합니다.
## 3. DB 변경 이력
실행용 경로: `backend/src/main/resources/db/migration`. 전체 59개 설계와 현재 실행용 11개 테이블은 구분합니다.
| 버전 | 변경 |
| --- | --- |
| V1 | 의약품 제형·제품·성분 3개 테이블 |
| V2 | 사전 시연 데이터 |
| V3 | users, user_profiles, user_consents, refresh_tokens |
| V4 | 소셜 계정, 이메일 인증·비밀번호 재설정 토큰 및 nullable 비밀번호 |
| V5 | auth_sessions 추가, refresh_tokens.session_id 연결, 기존 세션 없는 갱신 토큰 폐기 |
| V6 | user_profiles.onboarding_completed_at 추가, user_consents 버전 유니크 인덱스를 이력 인덱스로 교체하여 같은 버전 재동의 보존 |
V5 적용 후 기존 로그인 사용자는 다시 로그인해야 합니다. 실제 격리 MariaDB에서 V1~V6 적용과 스키마 검증을 확인했습니다. 실행용 테이블은 11개를 유지합니다.
## 4. 최신 테스트 결과
H2 실행 완료: 2026-10-14 20:33:37 KST. MariaDB 실행 완료: 2026-10-15 01:10:47 KST. Windows · Java 21.0.12.1. H2는 직전 실행의 Surefire 보고서를 확인했으며 10월 15일에 재실행한 것으로 기록하지 않습니다.
- H2: `mvnw.cmd test` · 59/59 통과 · Flyway 비활성, JPA create-drop.
- MariaDB 11.4.8: `scripts/Test-MariaDb.ps1` · 59/59 통과 · Flyway 활성, JPA validate.
- 두 실행 모두 실패 0, 오류 0, 건너뜀 0, BUILD SUCCESS.
| 테스트 클래스 | 개수 | 확인 범위 |
| --- | --- | --- |
| AuthFlowIntegrationTest | 32 | 기존 인증 22개와 프로필·온보딩·동의 변경 10개 |
| GoogleSocialIdentityProviderTest | 10 | 실제 RSA 서명 검증, 잘못된 대상·발급자·서명·만료·필수 클레임·이메일 거절 |
| KakaoSocialIdentityProviderTest | 7 | 토큰 앱 ID·만료·사용자 ID 대조, 미검증 이메일, 401·공급자 장애 |
| SocialAuthServiceTest | 6 | 신규 가입·기존 로그인·동의 누락·이메일 충돌·탈퇴·미지원 공급자 |
| SmtpAccountMailSenderTest | 2 | 루프백 SMTP 서버에 실제 전송, 이메일 인증/재설정 MIME 제목·본문·링크 검사 |
| HealthControllerTest | 1 | 상태 응답 |
| HealthHabitApiApplicationTests | 1 | 애플리케이션 기동 |
| 합계 | 59 | 각 환경 동일 테스트 구성 |
### 이번 검증에서 발견하고 수정한 사항
- 프로필 최초 저장 시 할당된 식별자로 JPA merge가 선택되어 500이 발생: MapsId가 신규 엔티티의 ID를 생성하도록 수정. 최종 두 DB에서 통과.
- 최초 온보딩 완료 응답과 재조회 시각 정밀도를 DATETIME(6)에 맞춰 마이크로초로 통일.
- 기본 프로필 미입력/선택값 삭제, 잘못된 입력과 저장 롤백, 반복 완료, 철회 후 차단·재동의 이력, 이전 버전 철회 충돌, 잘못된 동의 요청, 미인증 접근, 탈퇴 시 삭제, 동시 동의 및 사용자 간 격리를 검증.
- MockMvc 실패 출력에서 테스트 토큰을 노출하지 않도록 요청/응답 자동 출력을 비활성화.

아래는 2026-10-14 인증 보완 검증에서 수정한 사항입니다.
- 갱신 토큰이 저장되지 않은 객체를 참조하면서 정상 갱신이 500이 되던 문제: JPA save가 반환한 관리 객체를 연결하도록 수정.
- H2와 MariaDB 날짜 타입 차이로 만료 테스트 데이터가 다르게 해석되던 문제: 직접 JDBC 시각 입력 대신 JPA Instant 기반 테스트 데이터 변경으로 통일.
- 공급자 테스트 import 및 DTO 타입 어노테이션 컴파일 오류 수정 후 최종 전체 재검증.
### 결과의 한계
Google은 로컬 키로 서명한 토큰, Kakao는 모의 HTTP 응답을 사용했습니다. 실제 공급자 계정·클라이언트 설정·모바일 SDK 연동을 검증한 것은 아닙니다. SMTP는 localhost 수신 서버까지 검증했으며 외부 수신함 도착은 미검증입니다. 링크 화면은 라우팅·정적 자원·GET 비소비 동작을 확인했고 실제 브라우저 사용자 조작은 별도 확인 대상입니다.
MariaDB는 기존 DB와 분리한 임시 데이터·로컬 포트로 실행했으며 검사 후 서버를 종료했습니다. 재실행용 JDK·MariaDB 도구는 Git에서 제외한 .tools에 보관합니다.
빌드 중 Maven 저장소 메타데이터 401, Mockito 동적 에이전트, Windows MariaDB의 WSREP_ON 조회 경고가 있었으나 최종 검증은 성공했습니다.
## 5. 현재 동작 정책
- 이메일 가입은 PENDING_EMAIL_VERIFICATION → ACTIVE. 인증되지 않은 계정 로그인은 거절합니다.
- JWT의 sid와 DB 세션을 매 요청 확인합니다. 로그아웃·비밀번호 재설정·탈퇴 직후 기존 액세스 토큰을 차단합니다.
- 로그아웃은 현재 세션만, 재설정·탈퇴는 전체 세션을 폐기합니다. 이미 처리 중인 요청을 소급 취소하지는 않습니다.
- 세션 수명은 최초 로그인부터 기본 30일이고 갱신으로 연장하지 않습니다.
- 인증·재설정 메일 기본 발송 간격은 60초입니다. 제한 중에는 발송하지 않고 동일한 accepted 응답을 반환합니다.
- 새 메일 발급 시 이전 미사용 토큰을 무효화하고, 같은 토큰의 동시 제출은 한 번만 성공합니다.
- Google/Kakao 계정과 기존 이메일 계정을 자동 병합하지 않습니다. 충돌 시 기존 로그인 방식을 사용하게 합니다. 별도 계정 연결 API는 아직 없습니다.
- 탈퇴 시 외부 공급자 자체의 연결 해제 API는 호출하지 않습니다.
- /verify-email, /reset-password는 버튼으로 POST 요청을 보내며 GET만으로 토큰을 소비하지 않습니다. 토큰은 URL fragment로 전달하고 화면에서 URL을 정리합니다. 토큰을 브라우저 저장소에 보관하지 않습니다.
- 일반적인 로그인 시도 IP/계정별 제한은 아직 없으며, 이번 발송 제한과 구분합니다.
- 프로필 생략/null 입력은 기존값 유지, 선택값 삭제는 clearFields를 사용합니다. 운영의 non_null 직렬화에서는 미설정 응답 필드가 생략될 수 있습니다.
- 기본 온보딩은 닉네임과 필수 동의 3종으로 완료합니다. 생년월일·성별·키는 선택이며 몸무게는 신체측정 이력 범위입니다.
- 같은 약관 버전 철회 후 재동의는 새 행을 생성합니다. 기존 이력을 보존하고 현재 활성 동의만 미철회 상태로 유지합니다.
- 필수 동의 철회 후 프로필 수정·온보딩 완료는 차단하고 조회·재동의·탈퇴는 허용합니다. 철회 즉시 기존 프로필을 삭제하지는 않습니다.
- 약관 원문·게시 버전 레지스트리와 현재 버전 강제 검증은 미구현입니다. 버전 문자열은 기존 가입과 동일하게 클라이언트가 전달합니다.
## 6. 남은 작업
### 외부 서비스·클라이언트 연결
- [ ] GOOGLE_CLIENT_ID 및 KAKAO_APP_ID 설정 후 실제 앱 로그인 확인.
- [ ] MAIL_MODE=smtp 및 SMTP 정보·외부 HTTPS 링크 URL 설정 후 수신함 확인.
- [ ] Android SDK·토큰 보관·401 처리·로그인 복귀 흐름 연결.
### 후속 제품 개발
- [x] 프로필 수정·기본 온보딩·동의 변경.
- [ ] 생활패턴 설문·관심 카테고리, 신체측정 이력.
- [ ] 약관 게시 버전 관리, 동의 철회 시 기존 데이터 보존·삭제 정책, 향후 건강기록/분석 처리의 동의 검사 연결.
- [ ] 의약품 검색/상세 전용 테스트·공공데이터 적재·개인 복약 등록.
- [ ] 이후 확정 MVP의 식사·습관·알림·통계.
### 선택 확장
- [ ] 로그인 시도 제한, 계정 연결, 외부 공급자 연결 해제 정책.
## 7. 근거와 실행 안내
- `backend/AUTH_SETUP.md`: 설정값·이메일/소셜 시연·클라이언트 토큰 처리.
- `backend/PROFILE_ONBOARDING_API.md`: 프로필·기본 온보딩·동의 API 계약 및 앱 연결 순서.
- `backend/README.md`, `backend/CHANGELOG.md`, `product_specs/REST_API_명세서.md`: 현재 구현과 API 계약.
- `backend/src/main/java/com/routive/healthhabit/auth`, `config`, `user`: 구현.
- `backend/src/main/resources/static/auth-pages`: 링크 웹 화면.
- `backend/src/test/java/com/routive/healthhabit`: 테스트 소스.
- `backend/scripts/Test-MariaDb.ps1`: 격리 MariaDB 검증 스크립트.
- `backend/src/test/resources/application-mariadb-test.yml`: 실제 DB 검증 설정.
- `backend/target/surefire-reports`: 마지막 실행 보고서. 빌드 정리 시 삭제될 수 있어 이 문서에 결과를 보존합니다.
- `backend/DEVELOPMENT_LOG.md`: 노션 기록의 로컬 사본.
## 8. 앞으로의 기록 방식
개발 작업을 마칠 때 이 문서를 갱신합니다. 현재 상태는 최신 내용으로 유지하고, 아래 날짜별 작업 이력은 보존합니다.
기록 항목은 변경 목적, 구현 기능·API·DB, 실제 실행한 테스트 명령·환경·성공/실패 수, 미검증 항목, 다음 작업, 소스 근거입니다.
실행하지 않은 테스트나 외부 연동을 성공으로 기록하지 않습니다. 과거 결과를 재인용하면 실행일을 유지합니다. 노션 저장이 실패하면 로컬 기록에 동기화 대기를 남기고 다음 개발 작업에서 다시 반영합니다. 비밀키·토큰·개인정보 원문은 기록하지 않습니다.
이 갱신은 이 프로젝트에서 함께 개발하는 작업의 마무리 절차이며, 작업이 없는 동안 별도 주기 실행을 설정한 것은 아닙니다.
## 9. 날짜별 작업 이력
### 2026-10-14 · 백엔드 기반 및 인증 확장
- Spring Boot 골격, 공통 응답·오류·추적, 헬스 API, 의약품 사전 검색·상세와 시연 데이터 구현.
- 이메일 가입·동의·JWT 로그인·갱신·내 정보 조회 구현.
- 이메일 인증·재발송, Google/Kakao 소셜 로그인, 비밀번호 재설정, 로그아웃, 탈퇴 추가.
- Flyway V1\~V4와 실행 환경·API 문서 갱신.
- 마지막 자동 테스트 8개 전체 통과. 실제 외부 연동·MariaDB·Android 시연 검증은 남음.
- 개발·검증 기록을 노션 프로젝트와 로컬 파일에 정리하고 이후 누적 갱신 규칙 수립.
### 2026-10-14 · 인증 보완 및 실제 DB 검증
- 세션 기반 즉시 차단, 사용자 단위 잠금, 일회용 링크·재발송 제한, 갱신 토큰 회전 오류 수정.
- Google 검증 보완 및 Kakao 앱 ID 검증, 인증 오류 JSON 통일, 비밀번호 UTF-8 바이트 제한.
- 이메일 인증·재설정 링크 화면 및 SMTP 전송 검증 추가.
- V5 세션 스키마 추가. 실제 MariaDB와 기본 H2에서 각각 49개 테스트 통과.
- 개발 문서·API 명세·노션 기록 갱신. 실제 공급자 계정 및 앱 연동 검증은 남음.

### 2026-10-15 · 프로필·기본 온보딩·동의 변경 마무리
- 10월 14일 시작한 프로필 수정·온보딩 완료·동의 이력 조회/변경 API 및 V6 마이그레이션 구현을 마무리.
- H2 59개 통과는 10월 14일 20:33:37 보고서로 확인. 이번 실제 MariaDB 11.4.8 실행은 10월 15일 01:10:47, 59개 통과·실패/오류/건너뜀 0.
- MariaDB에서 V1~V6 적용과 JPA validate, 동시 동의 및 사용자 데이터 격리를 확인. 테스트 서버 종료.
- API 연결 문서·README·설정 안내·CHANGELOG·REST API 명세·인수인계·개발 기록 및 노션 갱신.
- 다음 작업: 의약품 사전 검증·공공데이터 적재·개인 복약 등록. Android 및 외부 실계정 연동은 미검증.

## 10. 이전 테스트 기록 (초기 구현, 현재 상태 아님)
<details>
<summary>2026-10-14 18:50 · 초기 8개 테스트 결과</summary>
	## 4. 테스트 결과 — 2026-10-14
	마지막 실행 완료: 2026-10-14 18:50 (Asia/Seoul)
	명령: backend 폴더에서 `mvnw.cmd test`
	환경: Windows · Java 21.0.12.1 · Spring Boot 4.0.8 · H2 메모리 DB의 MariaDB 호환 모드 · JPA create-drop · Flyway 비활성화.
	이번 문서 작성에서는 직전 실행의 Surefire 결과 파일과 테스트 소스를 다시 확인했습니다. 코드를 바꾸지 않았으므로 테스트를 재실행하지 않았습니다.
	| 테스트 클래스 | 개수 | 결과 | 실제로 검증한 내용 |
| --- | --- | --- | --- |
| AuthFlowIntegrationTest | 5 | 모두 통과 | 가입·인증·프로필 조회 / 중복 이메일 / 비밀번호 재설정 / 로그아웃 / 탈퇴 |
| SocialAuthServiceTest | 1 | 통과 | 가짜 공급자 인증 결과를 받아 신규 사용자 토큰 응답 생성 |
| HealthControllerTest | 1 | 통과 | 컨트롤러 직접 호출의 status=UP, service 값 |
| HealthHabitApiApplicationTests | 1 | 통과 | H2 환경의 애플리케이션 컨텍스트 시작 |
| 합계 | 8 | 실패 0 · 오류 0 · 건너뜀 0 | BUILD SUCCESS |
	### 인증 통합 테스트의 상세 판정
	1. `signupThenAccessMyProfileWithJwt`: 가입 대기 상태 확인 → 모의 메일 발송에서 인증 토큰 추출 → 인증 성공·액세스 토큰 발급 → JWT로 내 이메일·동의 3개 조회.
	2. `duplicateEmailIsRejected`: 같은 이메일 재가입 시 HTTP 409와 EMAIL_ALREADY_EXISTS 확인.
	3. `passwordResetChangesPasswordAndRevokesSessions`: 재설정 후 기존 갱신 토큰은 HTTP 401, 새 비밀번호 로그인은 성공.
	4. `logoutRevokesRefreshToken`: 로그아웃 후 기존 갱신 토큰 사용 시 HTTP 401.
	5. `withdrawalDisablesLogin`: 회원 탈퇴 후 기존 이메일·비밀번호 로그인 시 HTTP 401.
	### 결과의 범위
	- SMTP 발송기는 모의 객체입니다. 실제 수신함 도착이나 인증 링크 화면은 검증하지 않았습니다.
	- 소셜 테스트는 공급자 응답·저장소·토큰 서비스를 대체합니다. Google 서명·발급자·대상 검증 및 Kakao 실호출 성공을 증명하는 테스트가 아닙니다.
	- MariaDB에서 V1\~V4 적용 및 실제 조회 동작은 이 테스트 결과에 포함하지 않습니다.
	- 의약품 검색·상세와 Android 화면 연결은 별도 검증이 필요합니다.
	- 빌드 중 Flyway 관련 Maven 저장소 메타데이터 HTTP 401 경고와 Mockito 동적 에이전트 경고가 있었으나 이번 빌드·테스트는 성공했습니다.
	
</details>
