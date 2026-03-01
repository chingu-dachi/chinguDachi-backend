# 닉네임 중복 체크

## 엔드포인트
GET /api/users/check-nickname

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |

### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| nickname | String | Y | 중복 확인할 닉네임 |

### 닉네임 규칙
- 2~12자
- 공백 포함 불가

## 응답

### 사용 가능 (200 OK)
```json
{
  "code": null,
  "data": {
    "available": true
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 이미 사용 중 (200 OK)
```json
{
  "code": null,
  "data": {
    "available": false
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
