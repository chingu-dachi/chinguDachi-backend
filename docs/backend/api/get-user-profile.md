# 타인 프로필 조회

> PR #57 (BE-20)

## 엔드포인트
GET /api/users/{userId}

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |

### Path Parameters
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| userId | Long | 조회 대상 유저 ID |

## 응답

### 성공 (200 OK) — UserResponse (공개 프로필)
```json
{
  "code": null,
  "data": {
    "userId": "42",
    "nickname": "taro",
    "profileImageUrl": "https://...",
    "nationality": "JP",
    "translateLanguage": "ko",
    "birthYear": 1995,
    "interests": ["TRAVEL", "MUSIC"],
    "bio": "Hello!",
    "city": "Tokyo",
    "isOnline": false,
    "lastSeenAt": null,
    "createdAt": "2026-01-01T00:00:00Z"
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | String | 계정 ID (문자열) |
| nickname | String? | 닉네임 |
| profileImageUrl | String? | 프로필 이미지 URL |
| nationality | String? | 국적 코드 (KR, JP) |
| translateLanguage | String? | 번역 대상 언어 (소문자) |
| birthYear | Int? | 출생연도 |
| interests | List\<String\> | 관심사 태그 키 리스트 |
| bio | String? | 자기소개 |
| city | String? | 도시 |
| isOnline | Boolean | 접속 상태 (MVP: false) |
| lastSeenAt | Instant? | 마지막 접속 시간 (MVP: null) |
| createdAt | Instant | 계정 생성 시간 |

> `email`, `onboardingRequired`는 공개 프로필에 포함되지 않음 (MyProfileResponse에서만 제공).

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
| 404 | ACCOUNT_NOT_FOUND | 유저 없음 / 탈퇴 / 온보딩 미완료 |

## 비즈니스 규칙
- ACTIVE 상태 + `deletedAt == null` + 온보딩 완료 유저만 조회 가능
- 탈퇴/온보딩 미완료 유저는 ACCOUNT_NOT_FOUND 반환 (Privacy by design)
