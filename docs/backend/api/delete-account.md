# 회원 탈퇴

> PR #56 (BE-21)

## 엔드포인트
DELETE /api/users/me

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |

### Body
없음

## 동작
1. JWT에서 accountId 추출
2. `Account.softDelete()` 호출
   - `deletedAt` 설정 (현재 시각)
   - `accountStatus` → `INACTIVE`
   - `nickname` → `null` (닉네임 재사용 가능)
3. 관심사 삭제 (`userInterestStore.deleteByAccountId`)
4. 모든 세션 토큰 삭제 (`authTokenStore.deleteByAccountId`)
5. refreshToken 쿠키 클리어

## 응답

### 성공 (204 No Content)
```
HTTP/1.1 204
Set-Cookie: refreshToken=""; Path=/api/auth/refresh; HttpOnly; Secure; SameSite=Lax; Max-Age=0
```

Body 없음.

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
| 404 | ACCOUNT_NOT_FOUND | 계정을 찾을 수 없음 |
| 409 | ACCOUNT_ALREADY_DELETED | 이미 탈퇴한 계정 |

## 비즈니스 규칙
- **Soft delete**: DB 레코드 유지, `deletedAt` 타임스탬프로 삭제 표시
- 탈퇴 후 닉네임은 다른 유저가 사용 가능
- 탈퇴 후 해당 계정으로 재로그인 시 새 계정처럼 온보딩 필요
