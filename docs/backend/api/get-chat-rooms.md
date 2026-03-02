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
| cursor | Long | N | null (첫 페이지) | chatRoom.id 기반 커서 |

### 요청 예시
```
GET /api/chat-rooms
GET /api/chat-rooms?cursor=42
```

## 동작
- 내가 JOINED 상태인 ACTIVE 채팅방만 반환
- chatRoom.id DESC 정렬 (PK index 활용)
- 커서 페이지네이션: 내부 고정 size 20, `size + 1` 조회 → `hasMore` 판단
- N+1 해결: 배치 IN 쿼리 (3쿼리로 처리)

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
    "nextCursor": 42,
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

### 커서 타입: chatRoom.id (Long)
- PK index 활용으로 Instant 대비 성능 개선
- `(:cursor IS NULL OR cr.id < :cursor)` 패턴으로 쿼리 1개로 통합
- Long 타입은 PostgreSQL nullable 파라미터 타입 추론 이슈 없음

### size 파라미터 제거
- 내부 상수 20 고정 (PaginationConstants.DEFAULT_SIZE)
- 클라이언트가 size를 제어할 필요 없음

### N+1 쿼리 해결
- 배치 IN 쿼리 패턴 (3쿼리)
- 쿼리 1: 내 RoomMember + ChatRoom (JOIN FETCH, 커서 페이징)
- 쿼리 2: RoomMember WHERE chat_room_id IN (:roomIds)
- 쿼리 3: Account WHERE id IN (:accountIds)
- 상세: [chat-room-query-optimization.md](../chat-room-query-optimization.md)
