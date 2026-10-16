# SOL Android · 현서 1주차

Kotlin / Jetpack Compose / Material 3 기반 로컬 화면 데모입니다.
기준: [현서 — 프론트엔드](https://app.notion.com/p/3f8e6fa19a83800190e7cfc990c57b82).
진행 순서: 3.1 로그인·초기 설정 → 3.2 홈 → 이후 문서 순서.

## 현재 상태

Android Studio / JDK / Android SDK 설치 후 기존 데모는 사용자가 에뮬레이터에서 정상 동작을 확인했습니다.
승인한 SOL 디자인을 로그인·홈·습관 데모에 적용했습니다. 상세 범위와 에셋 출처는 `DESIGN.md`를 참고하세요.
회원가입 → 동의 선택 → 이메일 인증 체험 → 로그인 체험 → 프로필 설정 → 내 정보 데모를 연결했습니다.
실제 인증·API·영구 저장 기능은 없습니다. 이번 회원가입·내 정보 변경은 APK 빌드에 성공했으며 기기에서의 화면·동작 확인은 아직 하지 않았습니다.

## 회원가입 데모 사용하기

1. app을 다시 Run하고 로그인 화면의 회원가입을 누릅니다.
2. 예시 이메일 `demo@example.com`, 비밀번호 `demo123!`와 비밀번호 확인을 입력합니다. 실제 계정 정보를 입력하지 마세요.
3. 필수 동의 3개를 선택합니다. 마케팅은 선택 사항입니다. 상세 창은 원문 준비 상태를 안내하며 실제 약관 동의로 수집하지 않습니다.
4. 이메일 인증 완료 체험을 누릅니다. 실제 메일은 발송하지 않습니다. 재발송의 30초 제한은 데모용 설정입니다.
5. 로그인 체험에서 형식에 맞는 예시 비밀번호를 입력합니다. 실제 비밀번호 대조나 토큰 발급은 없습니다.
6. 닉네임을 설정합니다. 생년월일·성별·키는 선택 입력입니다. 완료하면 홈으로 이동합니다.
7. 하단 내 정보에서 입력 내용을 확인하고 수정할 수 있습니다. 데모 로그아웃 시 입력을 지웁니다.

입력과 프로필은 메모리에만 유지되어 프로세스 종료 시 사라집니다. 요청 실패 체험 스위치로 오류 안내와 재시도 흐름을 볼 수 있습니다.
3.1 상태: **프론트엔드 데모 구현 완료 · 성준 서버 연동 대기**. 현서의 합의에 따라 API·토큰·서버 연결은 성준이 추후 담당합니다. 게시 약관/버전 적용과 실제 인증·저장 완료는 연동 단계에서 확인합니다. 최신 디자인·비밀번호 수정본의 빌드 및 기기 검증은 별도 미완료 항목입니다. 다음 프론트엔드 개발 순서는 3.2 홈이며, 이 상태 정리에서 3.2 코드는 변경하지 않았습니다.

## 처음 실행하기

1. [Android Studio 공식 설치 안내](https://developer.android.com/studio/install)를 따라 Windows용 안정 버전을 설치합니다. 설치 프로그램과 SDK 라이선스는 직접 확인합니다.
2. 최초 설정 마법사에서 Android SDK를 설치합니다.
3. Android Studio의 **Open**에서 이 `android` 폴더를 선택합니다. 새 프로젝트를 만들 필요가 없습니다.
4. SDK Manager에서 **Android 15 / API 35**, SDK Build-Tools **35.0.0**, Platform-Tools를 설치합니다.
5. Gradle JDK는 Studio에 포함된 JDK 17 또는 21을 선택합니다. Gradle 배포는 Wrapper를 사용합니다.
6. Gradle Sync를 기다립니다. 이때 의존성이 다운로드되므로 인터넷이 필요합니다. 버전 자동 업그레이드 제안은 우선 보류합니다.
7. Device Manager에서 API 35 가상 기기를 만들거나 Android 8.0 이상 실제 기기를 연결합니다.
8. 실행 대상 `app`을 선택하고 Run을 누릅니다.

SDK 경로는 Studio가 로컬 설정으로 생성합니다. `local.properties`는 Git에 올리지 않습니다.
한글 경로에 대한 AGP 경고는 이 순수 Kotlin 프로젝트에서만 우회하도록 설정했습니다. NDK를 도입할 때 경로 호환성을 다시 확인해야 합니다.

## 고정한 개발 버전

| 항목 | 버전 |
|---|---|
| Android Gradle Plugin | 8.9.2 |
| Gradle Wrapper | 8.11.1 |
| Kotlin / Compose compiler plugin | 2.1.20 / 2.1.20 |
| Compose BOM | 2025.04.01 |
| compile / target SDK | 35 / 35 |
| 최소 SDK | 26 (팀 합의 전 개발용 가정) |

최신 버전이라는 의미가 아니라 재현 가능한 초기 조합입니다.
[AGP 호환표](https://developer.android.com/build/releases/agp-8-9-0-release-notes),
[Compose compiler 설정](https://developer.android.com/develop/ui/compose/compiler),
[Compose BOM 안내](https://developer.android.com/develop/ui/compose/bom).
배포 시점의 Play 요구사항은 별도 확인해야 합니다.

## 소스 읽는 순서

1. `MainActivity.kt`: 앱 시작, 화면 이동.
2. `core/ui/SolTheme.kt`: 색상과 글꼴. 시스템 글꼴을 사용합니다.
3. `core/ui/Components.kt`: 공통 버튼, 입력창, 카드.
4. `feature/auth/ui/LoginScreen.kt`: 입력 화면.
5. `feature/auth/viewmodel/LoginViewModel.kt`: 입력값과 오류 상태.
6. `feature/home/data/DemoHomeRepository.kt`: 화면 표시용 예시 데이터.
7. `feature/home/viewmodel/HomeViewModel.kt`: 홈 상태.

패키지 루트: `app/src/main/java/com/example/sol/`.
로그인은 입력 형식만 검사합니다. 데모 진입은 별도 버튼이며 인증 성공을 의미하지 않습니다.
비밀번호는 메모리에서만 유지하며 화면을 나갈 때 지웁니다. 프로세스 종료 시 입력은 사라집니다.

## 검증 상태와 남은 확인 항목

- JDK 21.0.11로 Debug APK 빌드 성공. 이번 변경의 자동 테스트와 Android Lint는 실행하지 않았습니다.
- 아래 기기 확인 항목은 이번 변경에 대해 아직 미실행입니다.
- 빈 이메일/비밀번호, 잘못된 이메일의 필드별 오류.
- 비밀번호 보기·숨기기, 키보드가 열린 상태에서 스크롤 및 버튼 접근.
- 데모 진입 → 각 안내 화면 → 뒤로 가기 → 데모 종료.
- 홈 빈 상태, 로딩 미리보기, 오류 → 다시 시도.
- 화면 회전, 큰 글꼴, 다크 모드, 작은 화면, TalkBack 읽기 순서.
- 에뮬레이터와 실제 기기 각각 실행.

빌드 명령: `./gradlew.bat :app:assembleDebug`
Lint 명령: `./gradlew.bat :app:lintDebug`
실행하지 않은 결과를 성공으로 기록하지 않습니다.

팀 저장소: https://github.com/HabbitSOL/routive-health-habit. develop 기준으로 codex/feat/android/signup-profile-demo 브랜치에서 PR을 준비했습니다.
브랜치 규칙은 루트 AGENTS.md와 development_roles/GitHub_브랜치_전략.md를 따릅니다.



## PR 준비 검증 · 2026-10-16

- Windows / JBR 21.0.11 / Android SDK 35에서 `:app:assembleDebug :app:lintDebug` 성공.
- Kotlin 컴파일 및 APK 생성 성공. Lint 오류 0, 경고 7 (의존성 최신 버전 안내 5, 백업 규칙 1, 앱 아이콘 1).
- 첫 offline Lint 시도는 캐시 누락으로 실패했고 온라인 의존성 다운로드 후 성공했습니다.
- 자동 테스트는 추가/실행하지 않았습니다. 기기 E2E, 실제 인증·메일·토큰·서버 저장은 미검증입니다.
- 실제 연동 담당은 성준이며 3.1은 프론트엔드 데모 구현 완료 · 서버 연동 대기입니다.

## 3.2 홈 대시보드

홈은 습관·최근 식사·다음 복약의 독립 상태와 빠른 기록 이동을 제공합니다. 기록 화면의 예시 완료 상태는 홈에도 반영됩니다. 상세 시연과 연동 기준은 HOME_HANDOFF.md를 참고하세요. 기존 3.1 이력 이후 추가된 내용입니다.


## 2026-10-16 · 3.2 검증 결과

- JBR 21.0.11 / SDK 35 / Windows 영문 임시 경로의 동일 소스에서 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline` 성공.
- HomeViewModel 단위 테스트 8개 통과, 실패/오류/건너뜀 0. 빈 상태·부분 성공·부분 오류·개별 재시도·최신 모드 우선·기록 반영·중복 요청·초기화를 확인했습니다.
- Lint 오류 0, 경고 2 (기존 백업 설정 및 앱 아이콘). offline 검사에서는 의존성 업데이트 안내가 제외될 수 있습니다.
- 기존 한글 경로에서는 컴파일 후 테스트 클래스 로딩 ClassNotFoundException 발생. 영문 경로의 동일 소스 검증으로 분리했습니다.
- 실제 기기 화면, 서버 API, 토큰, 실제 식사/복약 저장은 미검증입니다. 3.2 프론트엔드 데모 구현 완료 · 서버 연동/기기 확인 대기.
