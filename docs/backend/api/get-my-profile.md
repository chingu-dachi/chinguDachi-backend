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
    "userId": "1",
    "email": "user@example.com",
    "nickname": "테스트유저",
    "birthYear": 1995,
    "nationality": "KR",
    "translateLanguage": "ja",
    "city": "Seoul",
    "profileImageUrl": null,
    "bio": null,
    "onboardingRequired": false,
    "isOnline": false,
    "lastSeenAt": null,
    "createdAt": "2026-03-01T14:30:00Z",
    "interests": ["TRAVEL", "FOOD"]
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
    "userId": "1",
    "email": "user@example.com",
    "nickname": null,
    "birthYear": null,
    "nationality": null,
    "translateLanguage": null,
    "city": null,
    "profileImageUrl": null,
    "bio": null,
    "onboardingRequired": true,
    "isOnline": false,
    "lastSeenAt": null,
    "createdAt": "2026-03-01T14:00:00Z",
    "interests": []
  }
}
```

### 필드 변경 이력 (BE-29)

| 이전 | 이후 | 설명 |
|------|------|------|
| id (Long) | userId (String) | 문자열로 변환 |
| birthDate (LocalDate) | birthYear (Int?) | 연도만 노출 |
| nation | nationality | 필드명 변경 |
| translateLanguage "JA" | translateLanguage "ja" | 소문자 |
| accountStatus | onboardingRequired (Boolean) | 의미 명확화 |
| interests: [{tagKey, labelKo, labelJa}] | interests: [String] | tagKey만 |
| - | isOnline, lastSeenAt, createdAt | 신규 추가 |

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
| 404 | ACCOUNT_NOT_FOUND | 계정을 찾을 수 없음 |
