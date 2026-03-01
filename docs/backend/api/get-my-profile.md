# 내 프로필 조회

## 엔드포인트
GET /api/users/me

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |

### Body
없음

## 응답

### 성공 (200 OK)
```json
{
  "code": null,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "테스트유저",
    "birthDate": "1995-05-15",
    "nation": "KR",
    "nativeLanguage": "KO",
    "city": "Seoul",
    "profileImageUrl": null,
    "bio": null,
    "accountStatus": "ACTIVE",
    "interests": [
      { "tagKey": "TRAVEL", "labelKo": "여행", "labelJa": "旅行" }
    ]
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-01T14:30:00Z"
}
```

### 온보딩 미완료 유저
```json
{
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": null,
    "birthDate": null,
    "nation": null,
    "nativeLanguage": null,
    "city": null,
    "profileImageUrl": null,
    "bio": null,
    "accountStatus": "NOT_CONSENT",
    "interests": []
  }
}
```

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
| 404 | ACCOUNT_NOT_FOUND | 계정을 찾을 수 없음 |
