# 프로필 수정

> PR #57 (BE-20)

## 엔드포인트
PATCH /api/users/me

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |
| Content-Type | application/json |

### Body (Partial Update)
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| nickname | String? | N | 닉네임 (2~12자, 공백 불가) |
| bio | String? | N | 자기소개 |
| city | String? | N | 도시 |
| interests | String[]? | N | 관심사 태그 키 목록 |
| profileImageUrl | String? | N | 프로필 이미지 URL |

### 요청 예시
```json
{
  "nickname": "새닉네임",
  "bio": "수정된 자기소개",
  "interests": ["TRAVEL", "MUSIC", "FOOD"]
}
```

## 동작
- `null` 또는 미전달 필드는 변경하지 않음
- `interests` 전달 시 기존 관심사 전체 삭제 → 새로 저장 (replace 전략)
- `interests: []` (빈 배열) → 에러 (최소 1개 필수)
- `nickname` 변경 시 중복 체크
- `nationality`, `birthYear`는 수정 불가 (무시됨)

## 응답

### 성공 (200 OK) — MyProfileResponse
```json
{
  "code": null,
  "data": {
    "userId": "42",
    "email": "user@example.com",
    "nickname": "새닉네임",
    "birthYear": 1995,
    "nationality": "KR",
    "translateLanguage": "ja",
    "city": "Seoul",
    "profileImageUrl": null,
    "bio": "수정된 자기소개",
    "onboardingRequired": false,
    "isOnline": false,
    "lastSeenAt": null,
    "createdAt": "2026-01-01T00:00:00Z",
    "interests": ["TRAVEL", "MUSIC", "FOOD"]
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 400 | INTEREST_REQUIRED | interests가 빈 배열 |
| 400 | INTEREST_TAG_NOT_FOUND | 존재하지 않는 관심사 태그 키 |
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
| 404 | ACCOUNT_NOT_FOUND | 계정을 찾을 수 없음 |
| 409 | ACCOUNT_NICKNAME_DUPLICATE | 닉네임이 이미 사용 중 |
