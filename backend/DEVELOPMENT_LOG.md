# 백엔드 개발 현황 및 테스트 기록

노션 문서: https://app.notion.com/p/3f99280f6a7b817886c2e04f119a149c

> **공공 의약품 적재·갱신 코드 구현 및 로컬 DB 검증 완료**
> 기준일: 2026-10-15 · 담당: 성준 백엔드.
> 최종 로컬 테스트: 2026-10-15 H2·실제 MariaDB 11.4.8 각각 95개 통과. 실패·오류·건너뜀 0. Flyway V1~V7 및 JPA validate 성공. 식약처 실인증키·전체 실데이터 적재·Android 연결은 미검증.


## 1. 현재 개발 현황
| 영역 | 구현 상태 | 검증 및 남은 범위 |
| --- | --- | --- |
| 기반·사전 | 검색·상세, 시연 데이터, 문자 검색·동명 정렬·JSON 400 보완, 상세 imageUrl·interactions 추가 | 사전 통합 테스트 유지. 기본 데이터는 시연용 |
| 공공 의약품 적재 | e약은요 페이지 수집·품목코드 갱신·실행 이력·부분 재개·오래된 응답/동시 덮어쓰기 차단 | HTTP 모의/매핑 13개·DB 통합 11개 추가, 전체 95개 통과. 실인증키 미설정으로 외부 실호출 미검증 |
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
| GitHub·CI | HabbitSOL/routive-health-habit 공개 저장소, 초기 PR #1, H2·MariaDB CI | 사전 보완 원격 실행 각각 71개 통과. PR은 검토 대기. 초기 비공개 상태의 보호 설정 실패는 아래 날짜별 이력 참고 |
| 후속 제품 기능 | 개인 복약·식사·습관·알림·통계는 아직 미구현 | 다음은 식약처 실인증키 검증과 개인 복약 등록 |
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

의약품 상세에 imageUrl·interactions 필드를 추가했습니다. 공공 적재는 별도 사용자 HTTP API 없이 운영자 시작 설정으로 실행합니다. [PUBLIC_DRUG_IMPORT.md](PUBLIC_DRUG_IMPORT.md) 참고.
## 3. DB 변경 이력
실행용 경로: `backend/src/main/resources/db/migration`. 전체 59개 설계와 현재 실행용 12개 테이블은 구분합니다.
| 버전 | 변경 |
| --- | --- |
| V1 | 의약품 제형·제품·성분 3개 테이블 |
| V2 | 사전 시연 데이터 |
| V3 | users, user_profiles, user_consents, refresh_tokens |
| V4 | 소셜 계정, 이메일 인증·비밀번호 재설정 토큰 및 nullable 비밀번호 |
| V5 | auth_sessions 추가, refresh_tokens.session_id 연결, 기존 세션 없는 갱신 토큰 폐기 |
| V6 | user_profiles.onboarding_completed_at 추가, user_consents 버전 유니크 인덱스를 이력 인덱스로 교체하여 같은 버전 재동의 보존 |
| V7 | medication_products.interactions·record_version 추가, drug_import_runs 실행 이력 테이블 생성 |
V5 적용 후 기존 로그인 사용자는 다시 로그인해야 합니다. 실제 격리 MariaDB에서 V1~V7 적용과 스키마 검증을 확인했습니다. 실행용 테이블은 12개입니다.
## 4. 최신 테스트 결과
H2 실행 완료: 2026-10-15 18:14:21 KST. MariaDB 실행 완료: 2026-10-15 18:15:29 KST. Windows · Java 21.0.12.1.
- H2: `mvnw.cmd test` · 95/95 통과 · Flyway 비활성, JPA create-drop.
- MariaDB 11.4.8: `scripts/Test-MariaDb.ps1` · 95/95 통과 · Flyway 활성, JPA validate.
- 두 실행 모두 실패 0, 오류 0, 건너뜀 0, BUILD SUCCESS.
| 테스트 클래스 | 개수 | 확인 범위 |
| --- | --- | --- |
| AuthFlowIntegrationTest | 32 | 기존 인증 22개와 프로필·온보딩·동의 변경 10개 |
| GoogleSocialIdentityProviderTest | 10 | 실제 RSA 서명 검증, 잘못된 대상·발급자·서명·만료·필수 클레임·이메일 거절 |
| KakaoSocialIdentityProviderTest | 7 | 토큰 앱 ID·만료·사용자 ID 대조, 미검증 이메일, 401·공급자 장애 |
| SocialAuthServiceTest | 6 | 신규 가입·기존 로그인·동의 누락·이메일 충돌·탈퇴·미지원 공급자 |
| SmtpAccountMailSenderTest | 2 | 루프백 SMTP 서버에 실제 전송, 이메일 인증/재설정 MIME 제목·본문·링크 검사 |
| DrugDictionaryIntegrationTest | 12 | 공개 검색·상세, 이름/성분/업체, 문자 검색, 동명 제품 페이지 처리, 입력 오류, 비활성·없는 제품 |
| MfdsDrugClientTest | 13 | 키 인코딩·오류 비노출, HTTP/응답 실패, 페이지 건수, 메타데이터·HTML 변환 검증 |
| DrugImportIntegrationTest | 11 | 페이지 적재·반복·갱신·재개 지점, 부분 실패·원자성·낙관적 충돌·비활성 유지 |
| HealthControllerTest | 1 | 상태 응답 |
| HealthHabitApiApplicationTests | 1 | 애플리케이션 기동 |
| 합계 | 95 | 각 환경 동일 테스트 구성 |
### 이번 검증에서 발견하고 수정한 사항
- 공공 응답을 품목코드로 생성·갱신하고, 페이지 트랜잭션과 실행 이력 카운터가 함께 커밋/롤백되는지 검증.
- 날짜가 오래됐거나 미상인 응답이 확인된 최신 데이터를 덮어쓰지 않으며, 다른 트랜잭션의 동시 갱신은 record_version으로 충돌 처리.
- 빈 목록·수집 실패 시 기존 제품을 삭제하지 않고, 비활성 약품을 갱신해도 재활성화하지 않음.
- 반려 행이 있는 완료는 COMPLETED_WITH_REJECTIONS로 구분. 기본 서버 시작은 외부 적재를 실행하지 않음.
- 최초 테스트 컴파일에서 JUnit/Spring MediaType import 모호성을 수정한 뒤 최종 전체 검증.
- 긴 미종결 HTML에서 반복 정규식 탐색을 피하도록 선형 순회로 보완. 인코딩 태그·용량 비교 기호·큰 비정상 입력 테스트를 추가한 뒤 두 DB에서 전체 95개 재검증.
- 아래 사전·프로필 보완 항목은 이전 작업의 이력임.
- 검색어 %, _, !를 리터럴 문자로 이스케이프해 전체 검색으로 확대되는 현상 수정.
- 동명 제품에 ID 보조 정렬을 추가해 페이지 간 중복·누락 방지. JPA int 오프셋 범위를 넘는 요청은 400.
- 필수 쿼리 누락·숫자/UUID 변환 실패를 공통 JSON 400 INVALID_REQUEST로 처리.
- H2와 MariaDB 공용 테스트 전용 데이터 및 트랜잭션 롤백 사용. 출처 시각은 SQL 문자열 대신 JPA Instant 바인딩으로 검증.
- 초기 사전 실행은 데이터 준비 단계에서 H2 필수 active 값 누락으로 오류. 보완 후 11개 중 5개 실패를 확인하고 검색·정렬·오류 응답 및 날짜 테스트 데이터를 수정. size 상한 테스트를 추가한 최종 12개와 전체 71개가 두 DB에서 모두 통과.

아래 프로필·인증 검증은 이전 작업의 기록입니다.
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

### GitHub Actions 검증 — 2026-10-15

- 사전 보완 커밋 00fc510의 [PR 실행](https://github.com/HabbitSOL/routive-health-habit/actions/runs/38695826200)이 성공했습니다. 원격 H2·MariaDB 각각 71개 통과, 실패·오류·건너뜀 0, Flyway V1~V6 적용 및 JPA validate 성공.
- 아래 59개 결과는 초기 업로드 당시의 검증 이력입니다.

- 초기 원격 커밋 f0dc08f의 [Push 실행](https://github.com/1214alex/routive-health-habit/actions/runs/38597099261)과 [PR 실행](https://github.com/1214alex/routive-health-habit/actions/runs/38597237708)이 성공했습니다.
- 원격 Linux·Java 21에서 H2 59개, MariaDB 11.4에서 59개 통과. 실패·오류·건너뜀 0, 양쪽 BUILD SUCCESS.
- MariaDB 컨테이너의 빈 DB에 Flyway V1~V6 적용과 JPA validate를 확인했습니다.
- 초기 실행에서 발견한 Node 20·setup-java v4 지원 종료 경고에 따라 공식 checkout v7.0.1, setup-java v6.0.1을 SHA에 고정하고 Ubuntu 24.04로 변경했습니다. 갱신 후 결과는 PR의 최신 Checks에서도 확인할 수 있습니다.
- 갱신 커밋 a5d9d30의 [PR CI 실행](https://github.com/1214alex/routive-health-habit/actions/runs/38597607175)도 성공했습니다. 지원 버전 갱신 후 H2·MariaDB 각각 59개 통과와 V1~V6 적용을 재확인했습니다.
- 애플리케이션 코드를 변경하지 않아 이번 저장소 구성 중 로컬 테스트를 재실행하지 않았습니다. 위 원격 실행과 이전 로컬 결과를 구분합니다.
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
- 사전 검색은 제품명·성분 설명·업체명 부분 일치이며 특수문자는 실제 문자로 취급합니다. 이름/ID 순으로 페이지 처리하며 잘못된 쿼리·UUID는 JSON 400을 반환합니다.
- 공공 적재는 MFDS_IMPORT_ON_STARTUP=true인 운영자 설정에서만 실행합니다. 서비스키 원문 대신 고정 오류 코드만 기록하며 자동 삭제·자동 재활성화·무한 재시도는 하지 않습니다.
- e약은요 제공 범위는 일부 일반의약품입니다. 성분·외형·처방약·영양제 정보를 추정해 채우지 않으며 interactions는 출처의 일반 주의 설명입니다.
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
- [x] 의약품 검색/상세 전용 테스트 및 검색·페이지·입력 오류 보완.
- [x] e약은요 공공 적재·갱신 실행 코드·실행 이력·모의/DB 검증.
- [ ] MFDS 인증키·활용승인 후 외부 실호출·전체 데이터 적재 및 내용 확인.
- [ ] 성분·외형·처방약 등 추가 공식 출처 선정·매핑.
- [ ] 사전에서 개인 복약 등록 및 조회·수정·중지.
- [ ] 이후 확정 MVP의 식사·습관·알림·통계.
### 선택 확장
- [ ] 로그인 시도 제한, 계정 연결, 외부 공급자 연결 해제 정책.
- [ ] 현서의 GitHub 계정 확인 후 협업 권한 부여 및 초기 PR 검토·병합.
- [ ] 비공개 저장소 보호를 지원하는 GitHub 요금제 사용 시 main/develop 강제 보호 적용.
## 7. 근거와 실행 안내
- `backend/AUTH_SETUP.md`: 설정값·이메일/소셜 시연·클라이언트 토큰 처리.
- `backend/PROFILE_ONBOARDING_API.md`: 프로필·기본 온보딩·동의 API 계약 및 앱 연결 순서.
- `backend/DRUG_DICTIONARY_API.md`: 공개 사전 API 입력·검색·정렬·오류 정책과 검증 범위.
- `backend/PUBLIC_DRUG_IMPORT.md`: 공식 출처·실행 설정·갱신 규칙·실패 재개·검증 한계.
- `backend/src/main/java/com/routive/healthhabit/drug/importer`: 수집·매핑·실행 이력·페이지 저장 구현.
- `backend/README.md`, `backend/CHANGELOG.md`, `product_specs/REST_API_명세서.md`: 현재 구현과 API 계약.
- `backend/src/main/java/com/routive/healthhabit/auth`, `config`, `user`: 구현.
- `backend/src/main/resources/static/auth-pages`: 링크 웹 화면.
- `backend/src/test/java/com/routive/healthhabit`: 테스트 소스.
- `backend/scripts/Test-MariaDb.ps1`: 격리 MariaDB 검증 스크립트.
- `backend/src/test/resources/application-mariadb-test.yml`: 실제 DB 검증 설정.
- `backend/target/surefire-reports`: 마지막 실행 보고서. 빌드 정리 시 삭제될 수 있어 이 문서에 결과를 보존합니다.
- `backend/DEVELOPMENT_LOG.md`: 노션 기록의 로컬 사본.
- [GitHub 저장소](https://github.com/1214alex/routive-health-habit), [초기 PR #1](https://github.com/1214alex/routive-health-habit/pull/1), `development_roles/GitHub_저장소_시작하기.md`: 업로드·브랜치·초대 절차.
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

### 2026-10-15 · GitHub 초기 구성 및 전략에 따른 업로드

- 1214alex/routive-health-habit 비공개 저장소를 생성하고 origin을 연결. 작성자는 저장소 로컬 설정에 1214alex와 GitHub noreply 주소를 사용.
- 최초 기준 커밋으로 main/develop 생성. 초기 구현은 codex/chore/initial-import에서 develop 대상 PR #1로 업로드. 검토 전 병합하지 않음.
- type(scope): 변경 내용 형식으로 저장소 설정·설계 문서·백엔드 구현·CI를 분리해 커밋. 지원 종료 Action 갱신도 별도 커밋으로 기록.
- 루트 .gitignore, README, PR 템플릿, H2·MariaDB CI, 저장소 시작 안내 추가. 인증키·환경변수·로컬 도구·DB·빌드 결과·임시 출력물을 제외하고 업로드 목록 점검.
- 첫 Push 및 PR 원격 CI에서 각각 H2 59개·MariaDB 59개 통과. 실제 외부 계정·Android E2E는 기존 미검증 상태 유지.
- main/develop 보호 API는 GitHub Pro 또는 공개 전환이 필요하다는 HTTP 403 응답으로 미적용. 사용자 선택에 따라 비공개 상태를 유지하고 수동 협업 규칙으로 안내.
- 현서의 GitHub 사용자명은 미확정으로 초대하지 않음. 새 저장소는 GitHub 커넥터에서 404가 발생해 로그인된 GitHub CLI로 PR 생성.
- 개발 기록과 노션에 저장소·PR·검증 근거 및 제약을 기록.

### 2026-10-15 · 의약품 사전 검증 보강

- 공개 사전 API 통합 테스트 12개를 추가. 특수문자 검색, 동명 제품 페이지 중복, 쿼리 누락·숫자/UUID 오류의 500 응답을 재현하고 수정.
- 과도한 페이지 오프셋은 JPA int 범위를 확인해 400 처리. 기존 API 경로와 DB V1~V6·11개 테이블 유지.
- H2 15:57:25, 실제 MariaDB 11.4.8 15:58:27 KST에 각각 71개 통과, 실패·오류·건너뜀 0. 테스트 서버 종료.
- 초기 PR #1이 미병합이므로 기존 codex/chore/initial-import 작업 브랜치에 보완. main/develop에는 직접 Push·병합하지 않음.
- API 명세·README·CHANGELOG·인수인계·개발 기록·노션 갱신. 다음은 공공 데이터 적재와 개인 복약 등록.
- 이번 변경의 원격 CI는 PR #1의 최신 Checks에서 확인. 위 71개는 로컬 실행 결과이며 이전 원격 59개 기록과 구분.
- 원격 PR 실행 38695826200에서도 H2·MariaDB 각각 71개 통과. 저장소의 HabbitSOL 조직 이전을 확인하고 origin 주소를 갱신. 사용자가 현재 공개 설정 유지를 확인함.

### 2026-10-15 · 공공 의약품 적재·갱신

- feat/backend/public-drug-import 새 브랜치에서 작업. 이전 사용자 요청의 접두어 제거 규칙을 별도 docs(git) 커밋으로 보존.
- 공식 e약은요 명세를 확인해 JSON 페이지 수집·품목코드 갱신·반려 검증·실행 이력·부분 실패/재개 기능 구현.
- V7로 상호작용·낙관적 버전 및 실행 이력 추가. 상세 imageUrl·interactions 응답 확장.
- H2 18:04:46·실제 MariaDB 11.4.8 18:05:47 KST, 각각 94개 통과. 실패·오류·건너뜀 0. V1~V7와 JPA validate 성공 후 격리 서버 종료.
- HTML 변환 보완 후 최종 H2 18:14:21·MariaDB 18:15:29 KST, 각각 95개 통과. 실패·오류·건너뜀 0.
- 새로 추가한 HTTP 모의/매핑 13개·DB 통합 11개를 실제 제공자 실호출과 구분. MFDS_SERVICE_KEY 미설정으로 외부 인증·실데이터 적재는 미검증.
- 초기 PR #1 미병합 상태의 구현을 기반으로 하므로 [PR #2](https://github.com/HabbitSOL/routive-health-habit/pull/2)를 develop 대상 초안으로 생성. #1 선행 병합 후 브랜치 정리가 필요하며 임의 병합하지 않음.
- API·실행 안내·환경 예시·개발 기록·노션을 갱신. 다음은 실인증키 검증과 개인 복약 등록.

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
