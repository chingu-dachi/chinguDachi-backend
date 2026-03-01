# 토큰 리프레시

## 엔드포인트
POST /api/auth/refresh

## 인증
불필요 (permitAll) — refresh token은 쿠키로 전달

## 요청

### Cookie
| 이름 | 필수 | 설명 |
|------|------|------|
| refreshToken | O | 로그인 시 발급된 httpOnly 쿠키 |

### Body
없음

## 응답

### 성공 (200 OK)
```json
{
  "code": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...(새 토큰)",
    "onboardingRequired": false
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-01T14:30:00Z"
}
```

### Set-Cookie 헤더
기존 쿠키를 새 refresh token으로 교체 (토큰 로테이션)

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | AUTH_REFRESH_TOKEN_INVALID | 쿠키 없음 또는 이미 사용된 토큰 |
| 401 | AUTH_REFRESH_TOKEN_EXPIRED | 만료된 refresh token |

## 토큰 로테이션

리프레시 시 기존 토큰을 삭제하고 새 토큰을 발급합니다:
1. 쿠키에서 refresh token 추출
2. JWT 서명 + 만료 검증
3. DB에서 토큰 조회 (단일 사용 보장)
4. 기존 토큰 삭제 → 새 access + refresh 토큰 발급
5. 새 refresh token을 httpOnly 쿠키로 설정

## 에러 코드
- `AUTH_REFRESH_TOKEN_INVALID` — 쿠키가 없거나 DB에서 찾을 수 없는 토큰
- `AUTH_REFRESH_TOKEN_EXPIRED` — 만료된 refresh token (기존 토큰도 삭제됨)
