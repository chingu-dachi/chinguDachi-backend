# Google 로그인

## 엔드포인트
POST /api/auth/google

## 인증
불필요 (permitAll)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Content-Type | application/json |

### Body
```json
{
  "code": "google-authorization-code"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | String | O | Google OAuth authorization code |

## 응답

### 성공 (200 OK)
```json
{
  "code": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "onboardingRequired": true
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-01T14:00:00Z"
}
```

### Set-Cookie 헤더
```
refreshToken=eyJhbGci...; Path=/api/auth/refresh; HttpOnly; Secure; SameSite=Lax; Max-Age=2592000
```

| 속성 | 값 | 설명 |
|------|-----|------|
| HttpOnly | true | JS 접근 차단 (XSS 방지) |
| Secure | true | HTTPS만 전송 |
| SameSite | Lax | CSRF 방지 |
| Path | /api/auth/refresh | 리프레시 요청에만 전송 |
| Max-Age | 2592000 | 30일 |

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | AUTH_OAUTH_CODE_INVALID | 유효하지 않은 authorization code |
| 502 | AUTH_OAUTH_PROVIDER_ERROR | Google API 통신 실패 |

## 에러 코드
- `AUTH_OAUTH_CODE_INVALID` — 잘못되었거나 만료된 authorization code
- `AUTH_OAUTH_PROVIDER_ERROR` — Google 서버와의 통신 오류
