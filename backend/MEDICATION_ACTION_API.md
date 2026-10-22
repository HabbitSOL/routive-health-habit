# 실제 복약 행동 기록 — 1차

2026-10-19. `/api/v1`·Bearer 인증·활성 계정·현재 필수 동의 3종 필요.

| 메서드 | 경로 | 결과 |
| --- | --- | --- |
| POST | /medication-occurrences/{id}/actions | 최초 최종 행동 저장, 200 |
| GET | /medication-occurrences/{id}/actions | data.items 배열 0~1개, 쓰기 없음 |

POST에 UUID `Idempotency-Key` 헤더 필수. 본문 예시:

```json
{"action":"TAKE","actualAt":"2026-10-19T13:07:00+09:00","actualDoseAmount":1,"doseUnitCode":"TABLET","memo":null}
```

- TAKE: 실제 시각·실제 수량·단위 필수. 수량 0.001~999999999.999, 소수 3자리 이하. 회차 단위와 같아야 하며 예정량과 다른 양도 사실 기록으로 저장한다. 단위 환산·적정량 판단 없음.
- SKIP/NO_STOCK: action·선택 memo만 받는다. 실제 시각/수량/단위를 넣으면 400. 재고 수량을 자동 변경하지 않는다.
- memo 최대 1000자, 양끝 공백 제거, 빈 문자열은 null. 시각은 UTC·마이크로초 정밀도로 저장한다.
- 회차의 일정 시간대 기준 내일 이후 회차는 기록 불가(400). TAKE 실제 시각은 해당 회차 날짜의 시작 이상이며 서버 현재 시각 이하. 같은 날 예정 시각 전 복용 및 이후 지연 기록 가능. 지연을 의학적으로 판단하지 않는다.
- 중지/종료된 항목도 취소되지 않은 과거 회차는 늦게 기록 가능. TAKE 실제 시각은 중지/종료 시각을 넘을 수 없다. CANCELLED는 409.
- SCHEDULED → TAKEN/SKIPPED/NO_STOCK. 기록 후 회차 상태는 종결하며 동일 회차의 다른 키 기록은 409. 변경·삭제 API는 이번 범위에 없고 원본 유지/정정 이력은 후속.
- 같은 사용자·키·정규화 본문·회차 재요청은 원래 actionId/recordedAt 반환. 키는 사용자 내 모든 회차에서 유일. 다른 회차·다른 본문 재사용은 409. 동일 내용이라도 다른 키로 같은 회차 중복 기록은 409.
- 회원 잠금으로 생성·중지·동의 철회·탈퇴와 직렬화하고 DB 회차/요청키 UNIQUE로 보강한다. 모든 재전송도 현재 계정·동의·소유권 검사 후 처리.
- 다른 사용자/없는 회차 동일 404, 인증 없음401, 필수 동의 없음400. 잘못된 UUID/누락 키/입력 오류400.
- 응답: actionId, occurrenceId, action, status, actualAt(TAKE만), actualDoseAmount(TAKE만), doseUnitCode(TAKE만), memo(있을 때), recordedAt. 시간은 UTC 반환. 목록 회차 status에도 최종 상태가 표시된다.
- 과거 일정을 수정·중지해도 최종 행동을 덮어쓰지 않는다. 회원 탈퇴는 행동→회차→일정→개인 항목 순으로 삭제한다.
- V10 `medication_actions` 추가, 실행용 18개 테이블. 클라이언트는 서버 기록 시각·사용자ID·상태를 지정하지 않는다.
- 이번 범위 제외: PRN 자유 복용, 행동 정정, 자동 생성, 알림, 재고 계산, 실제 앱 E2E.
