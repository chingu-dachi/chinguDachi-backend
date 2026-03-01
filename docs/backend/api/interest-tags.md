# 관심사 태그 목록 조회

## 엔드포인트
GET /api/interest-tags

## 인증
불필요 (permitAll)

## 요청
파라미터 없음

## 비즈니스 규칙
- `displayOrder` 기준 오름차순 정렬
- 정적 데이터이므로 HTTP Cache-Control 적용 (max-age=3600, public)
- 온보딩 화면에서 관심사 선택 시 사용

## 응답

### 성공 (200 OK)
```json
{
  "code": null,
  "data": [
    {
      "id": 1,
      "tagKey": "TRAVEL",
      "labelKo": "여행",
      "labelJa": "旅行",
      "displayOrder": 1
    },
    {
      "id": 2,
      "tagKey": "FOOD",
      "labelKo": "음식",
      "labelJa": "食べ物",
      "displayOrder": 2
    }
  ],
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 응답 헤더
| 헤더 | 값 |
|------|------|
| Cache-Control | max-age=3600, public |
