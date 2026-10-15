# 의약품 사전 API

기준일: 2026-10-15. 공개 조회 API이며 로그인 토큰 없이 호출할 수 있다.
기본 데이터는 개발용 시연 데이터다. 공공 의약품 적재·갱신 실행 기능을 추가했으며 [공공 적재 안내](PUBLIC_DRUG_IMPORT.md)에 따라 별도 실행해야 한다. 실제 인증키로 전체 품목을 적재했다는 뜻은 아니다.

## 검색

GET /api/v1/drugs?keyword=검색어&page=0&size=20

| 항목 | 규칙 |
| --- | --- |
| keyword | 필수, 공백만 불가, 최대 100자. 양끝 공백 제거 후 대소문자 구분 없이 검색 |
| 검색 대상 | 활성 제품의 정규화된 제품명·성분 설명·업체명 부분 일치 |
| page | 0부터 시작, 기본 0, 음수 불가 |
| size | 기본 20, 1~100 |
| 정렬 | 제품명 오름차순, 같은 이름이면 제품 ID 오름차순 |
| 특수문자 | %, _, !를 검색 연산자가 아닌 실제 문자로 처리 |
| 최대 오프셋 | page × size가 2,147,483,647을 초과하면 400 |

성분 검색은 현재 ingredient_text 문자열 기준이다. 개별 성분 테이블의 이름만으로 검색 범위를 확장하지 않는다.

성공 응답은 공통 data 아래에 items, page, size, totalElements, totalPages, hasNext를 반환한다.
items에는 drugId, name, manufacturer, dosageForm, imageUrl이 포함된다.
운영의 non_null 직렬화에서는 미설정 필드가 생략될 수 있다.

- 결과가 없으면 200, items 빈 배열, totalElements/totalPages 0, hasNext false.
- 마지막 페이지를 넘으면 200과 빈 items를 반환하고 전체 검색 건수는 유지한다.
- 필수값 누락, 길이·숫자 범위 위반, 숫자 변환 실패는 400 INVALID_REQUEST.
- 입력 조건이 같고 데이터가 바뀌지 않으면 같은 이름의 제품도 ID 순으로 안정적으로 페이지 처리한다.

## 상세

GET /api/v1/drugs/{drugId}

활성 제품만 조회한다. 제품명·종류·업체·효능·사용법·주의사항·부작용·보관법,
외형·제형·성분 목록·출처·출처 갱신 시각·안내 문구를 반환한다.
V7 구현에서 imageUrl과 interactions 응답 필드를 추가했다. interactions는 출처가 제공하는 일반적인 약/음식 상호작용 주의 설명이다.
성분은 display_order, id 오름차순이다. 선택 정보가 없거나 성분 목록이 비어 있어도 조회할 수 있다.

- 존재하지 않거나 비활성인 제품: 404 DRUG_NOT_FOUND.
- UUID 형식이 아닌 제품 ID: 400 INVALID_REQUEST.
- 응답은 공통 JSON 형식과 X-Trace-Id를 사용한다.

## 자동 검증

DrugDictionaryIntegrationTest와 drug-dictionary-fixtures.sql에서 테스트 전용 데이터를 사용한다.
각 테스트의 트랜잭션을 롤백하여 Flyway 시연 데이터나 다른 테스트의 계정 데이터를 지우지 않는다.
출처 갱신 시각은 H2/MariaDB의 날짜 타입 차이를 피하도록 JPA Instant 바인딩으로 설정한다.

검증 범위:

- 공개 제품명 검색, 양끝 공백·대소문자 정규화
- 성분명·업체명 검색과 비활성 제외
- 총건수·페이지 수·다음 페이지·최대 size·범위 밖 페이지
- 동명 제품의 ID 순 페이지 처리
- 특수문자·SQL 형태 문자열의 일반 텍스트 검색
- 필수값·길이·숫자·범위 오류의 JSON 400
- 상세 외형·성분 순서·출처·UTC 시각과 선택값 누락
- 존재하지 않는/비활성 제품 404와 잘못된 UUID 400

실제 실행 환경·테스트 개수·결과는 DEVELOPMENT_LOG.md에 기록한다.
공공 적재 HTTP/DB 모의·통합 테스트는 별도로 추가했다. 실제 외부 API 연결·데이터 정확성·완전성 및 Android 화면 E2E는 아직 확인하지 않았다.
