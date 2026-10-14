# 루티브 AI 건강습관 서비스

습관·식사·복약 기록과 의약품 사전을 제공하는 서비스입니다.
성준은 Spring Boot 백엔드, 현서는 Android 앱을 담당합니다.

## 프로젝트 구조

| 경로 | 내용 |
| --- | --- |
| backend/ | Java 21 · Spring Boot · MariaDB REST API |
| product_specs/ | MVP 범위, 화면 흐름, REST API 계약 |
| development_roles/ | 역할, 아키텍처, GitHub 협업 규칙 |
| health_habit_*.md, *.sql, *.dbml, *.json | 전체 DB 설계와 데이터 사전 |
| CODEX_HANDOFF.md | 개발 인수인계 |

전체 DB 설계는 59개 테이블이며 현재 백엔드의 실행용 Flyway 스키마는 11개 테이블입니다.
실행·검증한 범위는 [개발 기록](backend/DEVELOPMENT_LOG.md)을 기준으로 확인합니다.

## 시작하기

1. JDK 21과 로컬 MariaDB 또는 Docker를 준비합니다.
2. [백엔드 실행 안내](backend/README.md)를 따라 실행합니다.
3. [인증 설정](backend/AUTH_SETUP.md)과 [프로필·온보딩 API](backend/PROFILE_ONBOARDING_API.md)를 확인합니다.
4. 작업 전 [브랜치·커밋·PR 규칙](development_roles/GitHub_브랜치_전략.md)을 읽습니다.

## 협업

- main: 시연·배포 기준
- develop: 기능 통합 기준
- 작업 브랜치 → develop PR → 검토·테스트 → 병합
- 커밋 형식: type(scope): 변경 내용

최초 저장소의 기준 커밋으로 main과 develop을 초기화합니다. 이후 구현 변경은 작업 브랜치에서 PR로 반영합니다.
Codex가 생성하는 작업 브랜치는 codex/ 접두어를 사용합니다.

환경변수 파일, 인증키, 로컬 DB·도구, 빌드 결과는 커밋하지 않습니다.
.env.example은 설정 예시이며 Spring Boot 실행 시 자동으로 읽히지 않습니다.
