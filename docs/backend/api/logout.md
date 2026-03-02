# 로그아웃

> PR #56 (BE-21)

## 엔드포인트
POST /api/auth/logout

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
2. 해당 계정의 refresh token 삭제 (`authTokenStore.deleteByAccountId`)
3. refreshToken 쿠키 클리어 (Max-Age=0)

## 응답

### 성공 (204 No Content)
```
HTTP/1.1 204
Set-Cookie: refreshToken=""; Path=/api/auth/refresh; HttpOnly; Secure; SameSite=Lax; Max-Age=0
```

Body 없음. `ApiResponseWrappingAdvice`는 `ResponseEntity`를 감싸지 않음.

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
