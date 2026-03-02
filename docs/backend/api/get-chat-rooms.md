# 채팅방 목록 조회

## 엔드포인트
GET /api/chat-rooms

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
| cursor | Instant (epoch millis) | N | null (첫 페이지) | lastMessageAt 기반 커서 |
| size | Int | N | 20 | 페이지 크기 (1~50) |

### 요청 예시
```
GET /api/chat-rooms?size=20
GET /api/chat-rooms?cursor=1709366400000&size=10
```

## 동작
- 내가 JOINED 상태인 ACTIVE 채팅방만 반환
- `lastMessageAt DESC NULLS LAST` 정렬 (최신 메시지 순)
- 커서 페이지네이션: `size + 1` 조회 → `hasMore` 판단

## 응답

### 성공 (200 OK) — PaginatedResponse\<ChatRoomResponse\>
```json
{
  "code": null,
  "data": {
    "items": [
      {
        "roomId": "1",
        "roomName": "나유저, 타겟유저",
        "participants": [
          {
            "userId": "1",
            "nickname": "나유저",
            "profileImageUrl": null,
            "nationality": "KR",
            "unreadCount": 0
          },
          {
            "userId": "42",
            "nickname": "타겟유저",
            "profileImageUrl": null,
            "nationality": "JP",
            "unreadCount": 3
          }
        ],
        "lastMessageAt": "2026-03-02T10:00:00Z",
        "createdAt": "2026-03-01T10:00:00Z"
      }
    ],
    "nextCursor": 1709366400000,
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

## 의사결정

### 커서 타입: Instant → epoch millis
- `nextCursor`는 Long (epoch millis)으로 반환
- PostgreSQL `NULLS LAST` 지원으로 lastMessageAt이 null인 방은 마지막에 정렬

### JPQL 쿼리 분리
- cursor null/non-null에 따라 별도 쿼리 — PostgreSQL이 nullable Instant 파라미터 타입 추론 불가 이슈 회피
