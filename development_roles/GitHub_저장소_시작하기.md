# GitHub 저장소 시작하기

생성일: 2026-10-15

- 저장소: https://github.com/HabbitSOL/routive-health-habit
- 소유자: HabbitSOL
- 공개 범위: 공개 (2026-10-15 조직 이전 후 사용자 유지 확인)
- 기본 브랜치: main
- 통합 브랜치: develop
- 최초 구현 업로드 브랜치: codex/chore/initial-import

## 다른 컴퓨터에서 받기

GitHub에서 접근 권한이 있는 계정으로 로그인한 뒤 실행한다.

```bash
git clone https://github.com/HabbitSOL/routive-health-habit.git
cd routive-health-habit
git switch develop
git pull --ff-only origin develop
git switch -c feat/backend/next-feature
```

최초 업로드 PR이 아직 병합되지 않았다면 구현 소스는 codex/chore/initial-import에서 확인한다.
초기 PR은 상대방 검토 후 develop으로 Squash Merge한다. main 반영은 develop에서 별도 릴리스 PR로 진행한다.
새 작업 브랜치에는 생성 주체와 관계없이 codex/ 접두어를 붙이지 않는다. feat/backend/next-feature처럼 유형·영역·작업명을 사용한다. 위 최초 업로드 브랜치명은 기존 이력으로 유지한다.

## 커밋과 PR

- 커밋: type(scope): 변경 내용
- 기능 PR: [BE] 기능명 또는 [APP] 기능명
- 기능 PR 대상: develop
- 기능 병합: Squash Merge
- 릴리스 PR: develop → main, Merge Commit
- 본인 PR은 상대방이 검토한다. CI 성공이 사람의 승인을 대신하지 않는다.

저장소에 이력이 없는 최초 1회만 기본 구조 커밋으로 main과 develop을 생성했다.
이후 직접 Push를 금지하며 GitHub_브랜치_전략.md의 규칙을 따른다.

## 자동 검증

Backend CI는 Java 21에서 다음 검사를 실행한다.

- Backend H2 tests: 전체 자동 테스트
- Backend MariaDB tests: MariaDB 11.4 서비스에서 Flyway 및 전체 자동 테스트

main/develop과 작업 브랜치의 백엔드 변경, main/develop 대상 PR, 수동 실행을 지원한다.
CI 전용 임시 DB를 사용하며 운영 자격 증명은 필요 없다.
원격 실행 결과는 GitHub Actions에서 확인한다. 로컬 테스트 결과와 구분한다.

## 팀원과 브랜치 보호

현서의 GitHub 사용자명이 확정되면 소유자가 Settings → Collaborators에서 접근 권한을 부여한다.
현재 문서의 이름만으로 계정을 추정해 초대하지 않는다.

GitHub 요금제에서 비공개 저장소 보호 규칙을 지원하면 main/develop에 PR 필수, 필수 CI, Force Push·삭제 금지를 설정한다.
main은 승인 1명과 대화 해결도 필수로 설정한다.
보호 규칙 적용 결과는 backend/DEVELOPMENT_LOG.md의 GitHub 초기 구성 이력을 확인한다.

2026-10-15 적용 시도 결과: main·develop 모두 GitHub Pro 업그레이드 또는 공개 저장소 전환이 필요하다는 HTTP 403 응답으로 강제 보호를 적용하지 못했다.
비공개 상태를 유지했다. 현재 PR·CI 절차는 팀 운영 규칙으로 준수해야 하며 GitHub가 직접 Push를 차단하는 상태는 아니다.

최초 검토 PR: https://github.com/HabbitSOL/routive-health-habit/pull/1

초기에는 개인 계정의 비공개 저장소로 생성했으며 이후 HabbitSOL 조직으로 이전되었다.
위 비공개 보호 실패는 초기 설정 당시의 이력이다. 현재 공개 저장소의 보호 설정 여부는 GitHub에서 별도 확인한다.

## 로컬 환경

- backend/.env.example은 예시만 제공한다. 실제 .env는 Git에 올리지 않는다.
- JDK·MariaDB 도구, 임시 DB, target, 출력물은 각 컴퓨터에서 준비한다.
- 비공개 저장소 접근 권한이 있어도 외부 Google/Kakao·SMTP 설정은 별도로 필요하다.
