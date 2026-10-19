# 개인 복약 기본정보 API — 1차 구현

후속 고정시간 일정·회차는 MEDICATION_SCHEDULE_API.md의 별도 API를 사용합니다. 아래 기본정보 생성/수정의 schedule 필드는 계속 미지원입니다.
일정이 존재하면 기본정보 startDate/endDate 수정은 409이며, 중지·종료 시 미래 예정 회차도 취소됩니다.

기준일: 2026-10-17. 모든 경로는 /api/v1 접두어와 Bearer 인증이 필요합니다.
이번 구현은 개인 항목 관리입니다. 반복 일정·회차·복용량·식후 계산·재고·알림·상호작용 판정은 포함하지 않습니다.
기존 REST 명세 6.14의 schedule 포함 예시는 후속 목표이며 현재 서버에 그대로 보내면 400입니다.

## 경로

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| POST | /medications | 등록, Idempotency-Key UUID 필수 |
| GET | /medications | 본인 목록, status 선택, page 기본 0, size 기본 20·최대 100 |
| GET | /medications/{id} | 본인 상세 |
| PATCH | /medications/{id} | ACTIVE 항목 부분 수정, version 필수 |
| POST | /medications/{id}/stop | STOPPED로 중지 |
| POST | /medications/{id}/complete | COMPLETED로 종료 |

성공은 현재 공통 응답 data/traceId와 HTTP 200입니다. 목록 data는 items, page, size, totalElements, totalPages, hasNext를 반환합니다.
기본 목록은 모든 상태이며 createdAt 내림차순·id 오름차순입니다. 상태 필터는 ACTIVE/STOPPED/COMPLETED만 지원합니다.

## 등록

```json
{
  "customName": "사용자가 입력한 이름",
  "medicationType": "SUPPLEMENT",
  "startDate": "2026-10-17",
  "endDate": "2026-11-17",
  "purpose": "사용자 메모",
  "memo": "참고 기록"
}
```

- drugId(사전 UUID) 또는 customName 중 정확히 하나. 이름은 255자 이하·공백 불가.
- 사전 등록은 활성 제품만 허용하며 medicationType은 제품 kind와 같아야 합니다. 표시 이름은 등록 당시 스냅샷입니다.
- 현재 유형은 PRESCRIPTION/OTC/SUPPLEMENT. SPORTS_SUPPLEMENT 및 PAUSED는 계획 명세에 있지만 이번 서버에서는 미지원(400).
- 시작일 필수, 종료일은 시작일보다 앞설 수 없음. 날짜는 시각이 아닌 LocalDate이며 약효·처방 유효성 판단을 하지 않습니다.
- purpose 최대 1000자, memo 최대 2000자. create에는 version·clearFields를 사용하지 않습니다.
- schedule이 null이 아닌 요청은 미지원이므로 400. 일정이 실제 생성된 것처럼 응답하지 않습니다.

응답: medicationId, drugId(연결 시), name, customName(직접 입력 시), medicationType, purpose, startDate, endDate, memo,
status, version(최초 0), createdAt, updatedAt, closedAt. null은 운영 직렬화에서 생략될 수 있습니다.

## 중복 요청

- 동일 사용자·Idempotency-Key·동일 정규화 본문은 기존 항목 ID와 현재 상태를 반환합니다.
- 같은 키로 본문을 바꾸면 409 DATA_CONFLICT. 사용자가 수정·중지한 이후 재전송해도 이전 상태로 되돌리지 않습니다.
- 요청키는 UUID로 정규화하고 본문 SHA-256을 보관합니다. 해시는 암호화나 익명화가 아닙니다.
- 사용자 잠금과 (user_id,request_key) 유니크 제약을 함께 사용합니다. 키는 항목 보존 기간 동안 유지합니다.
- 다른 키로 같은 약을 다시 등록하는 것은 허용합니다(별도 치료/관리 항목일 수 있음). 이 기능은 동일 요청의 재전송만 방지하며 약물 중복복용 판단이 아닙니다.

## 수정·중지·종료

```json
{
  "version": 0,
  "purpose": "수정한 목적",
  "clearFields": ["memo", "endDate"]
}
```

- 생략/null은 유지. purpose/endDate/memo 삭제는 clearFields 사용. 같은 필드에 값과 삭제를 동시에 지정하면 400.
- customName 변경은 직접 입력 항목에서만 가능. drugId·medicationType 교체 불가.
- 현재 version과 다르면 409. 성공한 수정 후 새 version을 화면에 저장해야 합니다.
- 중지·종료는 ACTIVE에서만 전환. 같은 종착 상태 반복 요청은 성공·무변경, 반대 종착 상태 또는 종착 상태 수정은 409.
- 재시작·일시정지·삭제 API는 미구현. 종료일이 지나도 자동 종료하지 않습니다.

## 보안·보존

- 본인 데이터만 조회/수정. 다른 사용자 ID와 없는 ID는 동일한 404 MEDICATION_NOT_FOUND.
- 계정 ACTIVE 및 현재 필수 동의 TERMS_OF_SERVICE/PRIVACY/SENSITIVE_HEALTH_DATA 확인.
- 필수 동의 철회 중에는 읽기·쓰기 모두 거절. 철회만으로 기존 항목을 즉시 삭제하지 않음.
- 회원 탈퇴 시 이 테이블의 본인 항목을 삭제. 중지/종료는 데이터를 보존합니다.
- 개인정보 원문은 요청/응답 테스트 로그에 자동 출력하지 않습니다.

## DB·검증

V8 personal_medications 추가. 사용자·제품 외래키, 사용자별 요청키 유니크, 사용자/상태/생성일 인덱스 및 JPA version 사용.
실행용 스키마는 V1~V8·13개 테이블입니다. 전체 계획 59개 테이블 설계 자체를 변경한 것은 아닙니다.
PersonalMedicationIntegrationTest에서 기본 등록/사전/재전송/사용자 격리/버전/상태/입력/인증/동의/탈퇴를 검증합니다.
실제 앱 E2E·다중 인스턴스 부하·동시 등록 경쟁을 별도 실측한 것은 아닙니다.
