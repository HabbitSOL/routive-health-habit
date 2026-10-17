# 고정시간 복약 일정·예정 회차 — 1차

2026-10-17. 개인 복약 기본정보(PR #5)에 의존합니다. 모든 경로 /api/v1·Bearer 인증·본인 데이터·현재 필수 동의 3종이 필요합니다.
식전/식후 계산, 실제 복용 행동, 재고, 알림, 자동 주기 생성은 이번 범위가 아닙니다.

## API와 연결 순서

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| POST | /medications/{id}/schedules | 새 고정 일정 버전 추가 |
| GET | /medications/{id}/schedules | 일정 이력·현재 medicationVersion 조회 |
| POST | /medications/{id}/occurrences?date=YYYY-MM-DD | 해당 하루의 예정 회차 저장 |
| GET | /medication-occurrences?date=YYYY-MM-DD | 본인의 저장된 회차 조회, 쓰기 없음 |

1. 개인 복약 등록 후 medicationId와 version을 받습니다.
2. 아래 일정 요청을 보냅니다. 응답 medicationVersion은 증가하므로 이후 수정에 새 버전을 사용합니다.
3. 원하는 날짜의 회차 생성 POST를 실행합니다. data.created는 신규 건수입니다.
4. 회차 GET으로 표시합니다. 생성 요청 전에는 빈 목록이며 성공적인 조회가 자동 생성/푸시를 뜻하지 않습니다.

## 일정 요청 예시

```json
{
  "medicationVersion": 0,
  "effectiveFrom": "2026-10-17",
  "timezone": "Asia/Seoul",
  "daysOfWeek": [1, 3, 5],
  "times": [
    {"sequence": 1, "fixedTime": "08:00", "doseAmount": 1, "doseUnitCode": "TABLET", "mealRelation": "NONE"}
  ]
}
```

- ACTIVE 항목만 허용. medicationVersion 불일치·종료 상태는 409.
- 요일은 ISO 월1~일7, 1~7개·중복 불가. 하루 슬롯 1~8개는 현재 입력/응답 크기 제한이지 의학적 복용 기준이 아닙니다.
- sequence는 1부터 빠짐없이 연속, 시간 중복 불가. fixedTime은 HH:mm만 허용합니다.
- doseAmount는 0.001~999999999.999, 소수점 3자리 이하. 저장 정밀도 제한이지 권장 복용량이 아닙니다.
- 단위: TABLET/CAPSULE/ML/MG/G/DROP/SACHET. 단위 변환·적정 용량 판단 없음.
- mealRelation은 생략/null/NONE만 허용. mealType/offsetMinutes는 생략/null. 식사 기반 입력은 400.
- 유효한 ZoneId 필수. timezone은 일정에 고정됩니다. 계정의 현재 시간대 변경이 기존 일정을 바꾸지 않습니다.
- effectiveFrom은 지정 시간대의 오늘 이후, 개인 복약 시작일 이후·종료일 이내입니다.

## 일정 버전·변경

- 첫 일정은 오늘부터 설정 가능. 이미 일정이 있으면 **내일 이후이며 마지막 버전 시작일보다 뒤**인 날짜에만 추가합니다.
- 마지막 일정의 effectiveUntil을 새 effectiveFrom으로 닫습니다. 종료 경계는 배타적입니다.
- 과거·당일 일정을 소급 수정하지 않으며 시간대 교체도 현재 미지원(400).
- 변경일 이후 기존 SCHEDULED 회차는 CANCELLED로 보존, 새 버전 회차는 명시적 생성 요청으로 만듭니다.
- 생성된 이름·시간대·용량·단위는 스냅샷입니다. 나중에 개인 항목 이름을 바꿔도 과거 회차가 바뀌지 않습니다.
- 요청 본문 재전송용 키는 일정 추가 API에 아직 없습니다. version으로 중복 변경을 거절하므로 응답 유실 시 일정 GET 후 현재 상태를 확인하세요.
- 일정이 생긴 개인 복약의 startDate/endDate 수정은 충돌 방지를 위해 409입니다. 별도 기간 재조정 API는 후속입니다.

## 회차 생성·조회

- 일정이 있는 ACTIVE 항목만 생성. 일정이 없으면 400입니다.
- 일정 시간대 기준 오늘~90일 후까지 하루씩 요청 가능합니다. 과거일·91일 이후는 400.
- 요일·개인 복약 시작/종료일(양끝 포함)·일정 버전 유효구간을 적용합니다. 해당 일정이 없는 날에는 created=0.
- 같은 일정ID/날짜/슬롯의 반복 생성은 0건·기존 ID 유지. 사용자 잠금과 DB 유니크로 보호합니다.
- 하루 슬롯 저장은 한 트랜잭션이며 오류 시 부분 저장하지 않습니다.
- 서머타임으로 존재하지 않거나 두 번 존재하는 시각은 자동 이동/선택하지 않고 400입니다.
- 회차 scheduledAt은 UTC Instant, scheduledDate는 **해당 일정 시간대의 달력 날짜**입니다. 여러 시간대 일정을 같은 date로 조회하면 각 일정의 해당 날짜 회차를 모읍니다.
- 응답 items: occurrenceId, medicationId, scheduleId, medicationName, scheduledDate, scheduledAt, timezone, mealRelation=NONE, doseAmount, doseUnitCode, status.
- status는 SCHEDULED/CANCELLED. CANCELLED도 반환하므로 앱은 취소 상태를 표시해야 합니다. 완료·미완료·실제 복용시간은 아직 없습니다.
- 중지/종료 시 현재 Instant 이후의 SCHEDULED 회차를 취소합니다. 이미 지난 예정 회차는 이력 유지이며 복용 완료로 간주하지 않습니다.
- 조회는 예정시각·ID 순. 회차 단위 페이지 처리는 아직 없습니다.

## 보안·삭제·DB

- 다른 사용자 개인 항목은 404. 날짜별 조회도 user_id로 제한합니다.
- 필수 동의 철회 시 일정·회차 읽기/쓰기 차단. 탈퇴 시 회차→일정 요일/슬롯→일정→개인 항목 순서로 삭제합니다.
- V9: medication_schedules, medication_schedule_days, medication_schedule_times, medication_occurrences.
- 사용자·개인 복약·일정 FK, 일정별 유효시작일 UNIQUE, 회차 일정/날짜/슬롯 UNIQUE 및 조회 인덱스.
- 실행용 V1~V9·17개 테이블이며 전체 계획 스키마와는 별개입니다.
- 실제 앱·자동 스케줄러·외부 서비스·멀티 인스턴스 부하·동시 경쟁 실측은 미검증입니다.
