# 유저 탐색

> PR #58 (BE-19)

## 엔드포인트
GET /api/users

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |

### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| nationality | String | N | null (전체) | 국적 필터 (KR, JP) |
| interests | String | N | null (전체) | 관심사 태그 키 (쉼표 구분) |
| cursor | String | N | null (첫 페이지) | 커서 기반 페이지네이션 |
| size | Int | N | 20 | 페이지 크기 (1~50) |

### 요청 예시
```
GET /api/users?nationality=KR&interests=TRAVEL,MUSIC&size=20
GET /api/users?cursor=42&size=10
```

## 동작
- 자기 자신 제외
- ACTIVE + `deletedAt == null` + 온보딩 완료 유저만 반환
- `nationality` 필터: Nation enum 매칭
- `interests` 필터: OR 매칭 (하나라도 일치하면 포함)
- `ORDER BY id DESC` (최신 가입 순)

### 커서 페이지네이션
- `size + 1`개 조회 → 초과분 존재 시 `hasMore = true`
- `nextCursor`: 마지막 반환 아이템의 `id` (문자열)
- 비숫자 cursor 값은 null로 처리 (안전한 `.toLongOrNull()`)

## 응답

### 성공 (200 OK) — PaginatedResponse\<UserResponse\>
```json
{
  "code": null,
  "data": {
    "items": [
      {
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
      }
    ],
    "nextCursor": "42",
    "hasMore": true
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 빈 결과
```json
{
  "code": null,
  "data": {
    "items": [],
    "nextCursor": null,
    "hasMore": false
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

## N+1 방지
- 결과 유저들의 interests를 `findByAccountIds(accountIds)` 배치 조회
- 유저 목록 조회 → ID 리스트 추출 → interests 일괄 조회 → 조합
