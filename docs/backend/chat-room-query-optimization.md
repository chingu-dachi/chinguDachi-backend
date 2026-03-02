# 채팅방 쿼리 최적화 & 기술 의사결정

채팅방 REST API (Phase 1) 구현 과정에서 검토한 기술적 의사결정 정리.

## 1. N+1 쿼리 해결

### 문제

GetChatRoomsUseCase에서 방 20개 조회 시:

```
쿼리 1: 내 RoomMember + ChatRoom (JOIN FETCH, 커서 페이징)
쿼리 N: 각 방마다 findByRoomId() — 20번
쿼리 2N: 각 멤버마다 accountStore.getById() — 40번 (1:1이니까 방당 2명)
총 61쿼리 (1 + 20 + 40)
```

### 방안 비교

| 방안 | 쿼리 수 | 장점 | 단점 |
|------|---------|------|------|
| A: 배치 IN 쿼리 | 3 | JPA 엔티티 그대로, 기존 패턴 일치 | 3쿼리 (vs 2) |
| B: 네이티브 SQL JOIN | 2 | 멤버+계정 한방 | 네이티브 SQL 의존, flat DTO 재구성 |
| C: @OneToMany + @EntityGraph | 2~3 | JPA 표준 | 양방향 관계 복잡성, Account는 별도 쿼리 |

### 선택: 방안 A (배치 IN 쿼리)

```
쿼리 1: 내 RoomMember + ChatRoom — JOIN FETCH, 커서 페이징
쿼리 2: RoomMember WHERE chat_room_id IN (:roomIds) — 모든 방의 멤버 한번에
쿼리 3: Account WHERE id IN (:accountIds) — 모든 멤버의 계정 한번에
→ 메모리에서 조합
```

근거:
- DiscoverUsersUseCase.loadInterests()와 동일 패턴 (코드베이스 일관성)
- 61 → 3쿼리 (95% 감소), 충분히 빠름
- JPA 엔티티 그대로 사용, 네이티브 SQL 불필요
- RoomMember-Account 간 JPA 연관관계 없어서 JPQL JOIN 불가 → 네이티브 SQL은 오버킬

---

## 2. 페이지네이션 메커니즘

### 커서 기반 페이지네이션 (Keyset Pagination)

offset 방식 대비 장점:
- 데이터 변경(새 채팅방 생성)에도 결과가 밀리지 않음
- PK index 활용으로 일관된 성능 (O(log n))

### 커서 타입 변경: Instant → chatRoom.id (Long)

**변경 전**: `lastMessageAt` (Instant, epoch millis)
```sql
-- cursor null/non-null에 따라 2개 쿼리 필요 (PostgreSQL nullable Instant 타입 추론 이슈)
-- 쿼리 1: cursor == null
SELECT rm FROM RoomMember rm JOIN FETCH rm.chatRoom cr
WHERE rm.accountId = :accountId AND rm.memberStatus = 'JOINED' AND cr.roomStatus = 'ACTIVE'
ORDER BY cr.lastMessageAt DESC NULLS LAST

-- 쿼리 2: cursor != null
SELECT rm FROM RoomMember rm JOIN FETCH rm.chatRoom cr
WHERE rm.accountId = :accountId AND rm.memberStatus = 'JOINED' AND cr.roomStatus = 'ACTIVE'
AND cr.lastMessageAt < :cursor
ORDER BY cr.lastMessageAt DESC NULLS LAST
```

**변경 후**: `chatRoom.id` (Long)
```sql
-- 단일 쿼리로 통합 (Long은 타입 추론 이슈 없음)
SELECT rm FROM RoomMember rm JOIN FETCH rm.chatRoom cr
WHERE rm.accountId = :accountId AND rm.memberStatus = 'JOINED' AND cr.roomStatus = 'ACTIVE'
AND (:cursor IS NULL OR cr.id < :cursor)
ORDER BY cr.id DESC
```

이점:
- PK index 직접 활용 (별도 인덱스 불필요)
- 쿼리 2개 → 1개로 통합 (분기 로직 제거)
- Long 타입은 PostgreSQL nullable 파라미터 타입 추론 이슈 없음
- nextCursor 값이 정수 (epoch millis 대비 직관적)

### size 파라미터 제거

- 클라이언트에서 size를 제어할 실제 니즈 없음
- 내부 상수 `PaginationConstants.DEFAULT_SIZE = 20` 고정
- `size + 1` 조회 → hasMore 판단 (21개 조회, 20개 반환)

### 조회 흐름 (GetChatRoomsUseCase)

```
1. findByAccountId(cursor, size=21) → 내 RoomMember + ChatRoom (JOIN FETCH)
   - 21개 반환 시 hasMore=true, 마지막 1개 drop
   - 20개 이하 → hasMore=false

2. roomIds 추출 → findByRoomIds(roomIds) → 모든 방의 RoomMember 배치 조회
   - groupBy { chatRoom.id } → Map<Long, List<RoomMember>>

3. accountIds 추출 → findByIds(accountIds) → 모든 Account 배치 조회
   - associateBy { id } → Map<Long, Account>

4. 메모리 조합 → ChatRoomResult 리스트 생성
   - nextCursor = 마지막 항목의 chatRoom.id
```

---

## 3. 1:1 채팅방 레이스 컨디션 해결

### 문제

CreateChatRoomUseCase에서 `findDirectRoom() → save()` 사이 TOCTOU 경쟁:
동시 요청 시 같은 두 유저 간 1:1 방이 2개 생길 수 있음.

### 방안 비교

| 방안 | 설명 | 적합성 |
|------|------|--------|
| A: direct_pair_key + Partial Unique Index | 1컬럼, DB 레벨 보장 | ✅ 선택 |
| B: member_id_min/max 2컬럼 | 숫자 2컬럼 + UNIQUE | 가능하나 비정규화 |
| C: Advisory Lock | pg_advisory_xact_lock | 스키마 변경 없으나 DB 종속적 |
| D: room_member UNIQUE(chat_room_id, account_id) | 보조 수단 | 핵심 문제 미해결 |
| E: SELECT FOR UPDATE | 비관적 락 | 레코드 없으면 적용 불가 |

### 선택: 방안 A (direct_pair_key) + 보조 D (room_member UNIQUE)

```sql
-- chat_room에 direct_pair_key 추가
ALTER TABLE chat_room ADD COLUMN direct_pair_key varchar(50);

-- Partial Unique Index (ACTIVE 방에만 제약)
CREATE UNIQUE INDEX uk_direct_chat_pair
ON chat_room (direct_pair_key)
WHERE room_status = 'ACTIVE';

-- 보조: 같은 방에 같은 유저 중복 방지
ALTER TABLE room_member ADD CONSTRAINT uk_room_member_room_account
UNIQUE (chat_room_id, account_id);
```

동작:
1. `directPairKey = "${minOf(a,b)}:${maxOf(a,b)}"` (예: "1:42")
2. DB INSERT 시 중복 → DataIntegrityViolationException
3. Controller에서 catch → 재실행 (기존 방 반환)
4. GROUP 채팅은 directPairKey = NULL (제약 없음)

### room_member만으로 해결 불가능한 이유

두 개의 동시 요청이 각각 **다른 chat_room_id**로 INSERT:
- room_member의 UNIQUE(chat_room_id, account_id)는 같은 방에 같은 유저만 막음
- 서로 다른 방에 같은 유저 쌍이 들어가는 것은 room_member만으로 제약 불가
- → 반드시 chat_room 레벨에서 중복 방지 필요

### GROUP 채팅에서 pair_key를 쓰지 않는 이유

기술적으로 가능: GROUP도 정렬된 멤버 ID로 pair_key 생성 가능 (예: "1:3:42")

실무에서 안 쓰는 이유:
- 멤버 변동(탈퇴/초대) 시 pair_key가 달라져 기존 방과 매칭 불가
- 같은 멤버라도 목적이 다른 그룹방 여러 개 생성 가능해야 함 (프로젝트A, 프로젝트B)
- 카카오톡도 같은 멤버 그룹방 중복 생성 허용

→ DIRECT만 pair_key 적용, GROUP은 NULL

---

## 4. PostgreSQL 기법 정리

### Partial Unique Index
```sql
CREATE UNIQUE INDEX uk_direct_chat_pair
ON chat_room (direct_pair_key)
WHERE room_status = 'ACTIVE';
```
- 특정 조건의 행에만 적용되는 유니크 인덱스
- ARCHIVED된 방은 제약에서 제외

### NULL과 UNIQUE
- PostgreSQL에서 NULL != NULL → 여러 NULL 행이 UNIQUE 위반 안 됨
- GROUP 채팅(directPairKey=NULL)이 여러 개 있어도 문제 없음

### nullable 파라미터 타입 추론
- PostgreSQL JDBC는 `IS NULL` 비교 시 Java null 값의 타입을 추론하지 못하는 경우 있음
- Instant (timestamp) → 이슈 발생 가능 → 쿼리 분리 필요했음
- Long (bigint) → 이슈 없음 → `(:cursor IS NULL OR cr.id < :cursor)` 단일 쿼리 가능

### MySQL이었다면?

MySQL은 Partial Index 미지원. 대안:
- **Generated Column + UNIQUE**: `ALTER TABLE chat_room ADD COLUMN active_pair_key VARCHAR(50) AS (IF(room_status='ACTIVE', direct_pair_key, NULL)) STORED, ADD UNIQUE (active_pair_key);` — MySQL 5.7.6+
- **별도 매핑 테이블** (direct_chat_pair): 가장 범용적
- **Trigger 기반**: 복잡하고 유지보수 어려움

PostgreSQL의 Partial Index가 가장 깔끔.

---

## 5. DDL 변경사항

```sql
-- chat_room 스키마 변경
ALTER TABLE chat_room ADD COLUMN room_type varchar(10) NOT NULL DEFAULT 'DIRECT';
ALTER TABLE chat_room ADD COLUMN direct_pair_key varchar(50);

-- 기존 member_id_low/member_id_high 컬럼 제거 (direct_pair_key로 대체)
ALTER TABLE chat_room DROP COLUMN IF EXISTS member_id_low;
ALTER TABLE chat_room DROP COLUMN IF EXISTS member_id_high;

-- 1:1 방 중복 방지 (Partial Unique Index)
CREATE UNIQUE INDEX uk_direct_chat_pair
ON chat_room (direct_pair_key)
WHERE room_status = 'ACTIVE';

-- room_member 보조 보호
ALTER TABLE room_member ADD CONSTRAINT uk_room_member_room_account
UNIQUE (chat_room_id, account_id);
```
