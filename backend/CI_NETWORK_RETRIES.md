# 백엔드 CI 네트워크 재시도

2026-10-22. `chore/backend/ci-network-retries`에서 develop 기준으로 분리했다.

- H2/MariaDB 작업에 checkout 전 GitHub·Maven Central 연결 확인을 추가한다. 각각 연결 5초·요청 15초 제한, 최대 4회, 실패 사이 10초 대기.
- checkout 후 Maven Wrapper 준비는 `timeout 120s bash mvnw --batch-mode --version`을 최대 3회 시도한다. 실제 테스트는 한 번 실행한다.
- 네트워크/도구 준비 실패와 테스트 실패의 오류 메시지를 구분한다.
- 기존 backend 경로 필터, 문서 전용 제외, Android 워크플로, Action SHA 고정, contents:read 및 테스트 명령을 유지한다.
- 서비스 컨테이너 이미지 다운로드, setup-java 자체 다운로드 또는 checkout 이후 일어나는 모든 네트워크 실패를 재시도하는 기능은 아니다. 영구 장애나 존재하지 않는 Maven 버전을 해결하지 않는다.

검증: 2026-10-22 공식 rhysd/actionlint v1.7.12 Windows 릴리스의 게시된 SHA256을 대조한 뒤 `actionlint -shellcheck= -pyflakes= .github/workflows/backend-ci.yml` 통과. 로컬 shellcheck/pyflakes는 미설치로 제외했다. 실제 Linux 실행 결과는 이 PR의 GitHub Actions에서 확인하며 DNS 장애를 강제로 주입한 실험은 하지 않았다.

API·DB 변경 없음. 앱/운영 배포 검증과 구분한다.
