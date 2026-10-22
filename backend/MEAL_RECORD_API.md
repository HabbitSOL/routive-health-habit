# 식사 기록·6개 영양값 — 1차

2026-10-22. `/api/v1`, Bearer 인증·활성 계정·필수 건강정보 동의 필요.

| 메서드 | 경로 | 내용 |
| --- | --- | --- |
| POST | /meals | UUID Idempotency-Key로 완료한 식사 직접 입력 |
| GET | /meals?from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20&status=COMPLETED | 기간·상태별 목록 |
| GET | /meals/{id} | 본인 식사의 음식·당시 영양값 |
| DELETE | /meals/{id}?version=0 | 기록 취소, 원본 보존 |
| GET | /statistics/meals?from=YYYY-MM-DD&to=YYYY-MM-DD | 취소되지 않은 완료 식사 합계 |

## 입력 계약

```json
{
  "mealType":"LUNCH",
  "eatenAt":"2026-10-22T12:30:00+09:00",
  "status":"COMPLETED",
  "memo":null,
  "items":[{
    "foodName":"닭가슴살",
    "amount":150,
    "unitCode":"G",
    "nutrients":{"caloriesKcal":165,"carbohydrateG":0,"proteinG":31,"fatG":3.6,"sugarG":0,"caffeineMg":null}
  }]
}
```

- 첫 단계는 완료 식사·직접 입력만 지원. mealType은 BREAKFAST/LUNCH/DINNER/SNACK/LATE_NIGHT. items 1~30개, 이름 255자, memo 1000자. 양끝 공백 제거, 빈 음식명 거절.
- eatenAt 필수, 1970-01-01T00:00:00Z 이상·서버 현재 이하. UTC 마이크로초 저장. 서버 recordedAt과 분리하며 사용자 시간대와 해당 시각의 mealDate를 스냅샷으로 저장한다.
- 수량 0.001~999999999.999, 소수 3자리 이하. G/ML/SERVING/PIECE만 지원하며 단위 변환은 하지 않는다.
- **nutrients는 실제 입력한 섭취량 전체의 영양값**이다. 100g당 값이 아니며 amount를 곱하지 않는다. kcal, 탄수화물/단백질/지방/당류 g, 카페인 mg. 단백질 보충제 등도 실제 제품의 6개 값만 입력한다.
- 영양값은 각 0~999999999.999, 소수 3자리 이하. 0은 확인한 0, null/생략은 모름. nutrients 전체 생략도 모름으로 저장한다. 당류는 총당류이며 첨가당/유리당 자동 분리는 하지 않는다.
- foodId·imageFileKey는 null/생략만 허용하며 다른 값은 400. 음식 사전·사진 업로드·AI 추정·식후 복약 재계산은 후속이며 성공한 것처럼 응답하지 않는다.

## 합계와 재전송

- 응답 totals는 영양값 하나라도 미상인 음식이 있으면 그 영양값을 null로 반환한다. knownTotals는 확인된 값만 더한 소계이고 missingItemCounts는 영양값별 미상 음식 수다. 미상 값을 0으로 가정하지 않는다.
- 통계는 저장된 mealDate 기준 from~to 양끝 포함, 최대 366일. 완료 상태만 포함한다. 빈 기간 mealCount/itemCount 0, 합계 0과 미상 수 0을 반환한다. 이전 기록을 실제로 먹지 않았다고 추정하지 않는다.
- 목록 size 1~100, page 0 이상·offset 최대 100000. 기본 COMPLETED, CANCELLED 조회도 지원. 예정시각이 아닌 eatenAt 내림차순·ID 순서. 사용자별 데이터만 반환한다.
- 같은 사용자·요청키·정규화 내용은 같은 mealId/recordedAt 반환. 시각 표현·숫자 소수 표기·양끝 공백은 정규화한다. 다른 내용 재사용 409, 음식 순서도 내용의 일부다. 다른 키의 비슷한 식사는 자동으로 중복 판정하지 않는다.
- 취소는 version 충돌 409, 이미 취소한 요청은 기존 결과 반환. 원본 음식과 영양값은 남기고 통계에서 제외한다. 취소 후 생성 요청 재전송도 취소된 원본을 반환하며 되살리지 않는다.
- 저장/상세/취소 응답: mealId, mealType, eatenAt, recordedAt, mealDate, timezone, status, memo, cancelledAt, version, items, totals, knownTotals, missingItemCounts. items는 itemId, sequence, foodName, amount, unitCode, nutrients, source=USER_ENTERED. 선택 메모/취소 시각은 없으면 생략하며 6개 영양값의 null은 명시적으로 반환한다.
- 목록은 data.items/page/size/totalElements/totalPages/hasNext, 통계는 data.from/to/mealCount/itemCount/totals/knownTotals/missingItemCounts. 취소 API도 200으로 최종 원본을 반환한다.
- 수정/재개/물리 삭제 API는 후속. 탈퇴는 음식 항목→식사 순으로 실제 삭제. 동의 철회 시 읽기/쓰기/재전송 모두 차단.

## DB·병합 순서

- V11 meals/meal_items 신규. V10은 복약 PR #10에 예약되어 있으므로 **PR #10을 먼저 병합**해야 한다. 식사 PR은 이 조건이 충족될 때까지 초안으로 유지한다.
- 최초 독립 검증(15:28/15:31 KST)은 V1~V9+V11·19개 실행용 테이블이었다. 2026-10-22 리뷰 수정에서 PR #10의 `75fd7f8`을 작업 브랜치에 반영하여 현재 V1~V11·20개를 포함한다. 기존 SQL 파일의 번호·내용을 변경하지 않는다.
- UserAccountService 생성자와 삭제 처리, 오류 코드, README 충돌을 해결해 복약 행동 삭제와 식사 삭제를 모두 보존했다. 완료/취소 식사와 복약 행동을 함께 가진 계정의 HTTP 탈퇴 및 다른 사용자 데이터 보존 회귀를 추가했다.
- PR #10을 먼저 develop에 병합한 뒤 최신 develop을 이 브랜치에 반영하고 전체 테스트를 다시 확인한 뒤 초안을 해제한다. 작업 브랜치끼리 통합한 것은 develop 병합이 아니다. 앞서 V11만 적용했던 테스트 DB는 재사용하지 않고 빈 격리 DB에서 V1~V11을 검증한다. V11 적용 후 V10을 뒤늦게 적용하거나 Flyway out-of-order/repair로 우회하는 배포는 하지 않는다.
- 건강정보 접근 검사를 공통 서비스로 분리하고 기존 복약 검사의 동작은 유지한다. 사용자 잠금과 DB 사용자/요청키 UNIQUE로 동시 생성·취소·동의 철회·탈퇴를 직렬화한다.
- 실제 앱 E2E·영양값의 사실 검증·외부 음식 API·사진·식사 일정/건너뜀·목표/AI·알림은 이번 범위가 아니다.

## 검증 이력

- 2026-10-22 Windows·Java 21.0.12.1·가짜 데이터. H2 `mvnw.cmd --batch-mode -o test` 15:28:52 KST 전체 141개 통과.
- `scripts/Test-MariaDb.ps1`에 공용 도구 경로를 지정, 15:31:02 KST 격리 MariaDB 11.4.8 전체 141개 통과. 실패/오류/건너뜀 0, V1~V9+V11·JPA validate 성공. 서버 종료.
- 신규 식사 테스트 22개 포함. 과거 조합본 156개나 다른 PR의 147개와 같은 소스가 아니므로 테스트 수를 완료율로 비교하지 않는다.
- 최초 테스트 컴파일/캐시 기반 탈퇴 검증 실패와 수정 내역은 DEVELOPMENT_LOG.md에 보존한다. 이 최초 독립 검증 당시 실제 앱·외부 공급자 및 PR #10과 통합은 미검증이었다. 이후 통합 결과는 아래에 구분한다.

### 리뷰 후 복약 PR #10 통합 검증 · 2026-10-22

- PR #10 `75fd7f8` 반영 후 신규 동시 보유 계정 탈퇴 회귀 1개를 추가했다. 실제 복약 행동과 완료/취소 식사, 자식 데이터 삭제·다른 사용자 보존·기존 토큰 차단을 검증한다.
- Windows·Java 21.0.12.1·가짜 데이터. H2 `mvnw.cmd --batch-mode -o test`: 16:10:13 KST 170개 통과. 격리 MariaDB 11.4.8 `scripts/Test-MariaDb.ps1`(공용 도구 경로 지정): 16:10:57 KST 170개 통과, 실패/오류/건너뜀 0. 빈 DB의 Flyway V1~V11 순차 적용 및 JPA validate 성공, 서버 종료.
- 첫 H2 16:09:10 KST 신규 테스트 실패는 HTTP 탈퇴 필수 확인 본문 누락으로, 기존 계약에 맞춰 테스트를 보완했다. 이전 독립 브랜치 141개 결과와 이번 통합 170개 결과를 구분한다.
- PR #8/#9 포함 최종 조합·실제 외부 서비스·Android E2E·배포는 미검증이다. #10 선행 병합 후 develop 재반영·원격 CI 확인 전까지 초안 유지.
