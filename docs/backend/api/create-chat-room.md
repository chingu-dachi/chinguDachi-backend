# 채팅방 생성

## 엔드포인트
POST /api/chat-rooms

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |
| Content-Type | application/json |

### Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| targetUserId | Long | Y | 채팅 대상 유저 ID |

### 요청 예시
```json
{
  "targetUserId": 42
}
```

## 동작
- 자기 자신에게 생성 시도 → 400 (CHAT_ROOM_SELF_CHAT)
- 대상 유저가 ACTIVE가 아닌 경우 → 400 (CHAT_ROOM_TARGET_NOT_ACTIVE)
- 이미 두 유저 간 ACTIVE 채팅방 존재 시 → 기존 방 반환 (멱등)
- 없으면 새 채팅방 + RoomMember 2명 생성
- roomName: "나닉네임, 상대닉네임" 형식

## 응답

### 성공 (201 Created)
```json
{
  "code": null,
  "data": {
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
        "unreadCount": 0
      }
    ],
    "lastMessageAt": null,
    "createdAt": "2026-03-02T10:00:00Z"
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 400 | CHAT_ROOM_SELF_CHAT | 자기 자신에게 채팅방 생성 |
| 400 | CHAT_ROOM_TARGET_NOT_ACTIVE | 대상 유저가 비활성 상태 |
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
| 404 | ACCOUNT_NOT_FOUND | 대상 유저가 존재하지 않음 |

## 의사결정

### 멱등성
- 같은 두 유저 간 중복 생성 요청 시 기존 방을 반환
- JPQL 서브쿼리로 양쪽 모두 JOINED 상태인 ACTIVE 방 검색

### ID 타입
- 프론트엔드 계약: ID는 string — `Long.toString()` 변환
