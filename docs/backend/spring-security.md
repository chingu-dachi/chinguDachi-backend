# Spring Security 설정

## 아키텍처

```
요청 → MdcLoggingFilter → JwtAuthenticationFilter → SecurityFilterChain → Controller
```

## JwtAuthenticationFilter

`OncePerRequestFilter`를 상속한 JWT 인증 필터.

### 동작 흐름
1. `Authorization: Bearer {token}` 헤더에서 토큰 추출
2. `TokenProvider.validateToken()`으로 서명 + 만료 검증
3. 유효하면 `parseAccountId()`로 accountId 추출
4. `UsernamePasswordAuthenticationToken.authenticated(accountId, null, emptyList())`로 인증 객체 생성
5. `SecurityContextHolder`에 설정
6. MDC에 userId 업데이트 (로깅용)

### 토큰이 없거나 유효하지 않은 경우
SecurityContext를 설정하지 않고 필터 체인을 계속 진행. Spring Security의 authorization 체크에서 401 반환.

## SecurityConfig 경로 규칙

| 경로 | 메서드 | 권한 | 설명 |
|------|--------|------|------|
| `/api/auth/google` | POST | permitAll | Google OAuth 로그인 |
| `/api/auth/refresh` | POST | permitAll | 토큰 갱신 |
| `/api/interest-tags` | GET | permitAll | 관심사 태그 목록 |
| `/api/users/check-nickname` | GET | permitAll | 닉네임 중복 확인 |
| `/error` | ALL | permitAll | Spring 에러 페이지 |
| 그 외 모든 경로 | ALL | authenticated | JWT 인증 필요 |

> **변경 이력 (BE-21)**: 기존 `/api/auth/**` 와일드카드를 `POST /api/auth/google`, `POST /api/auth/refresh`로 분리.
> `POST /api/auth/logout`은 인증 필수 (누구의 토큰을 폐기할지 알아야 하므로).

## SecurityContextUtil

`SecurityContextHolder`에서 accountId를 추출하는 유틸리티:

```kotlin
val accountId = SecurityContextUtil.getCurrentAccountId()
```

인증되지 않은 상태에서 호출하면 `UnauthorizedException` 발생.

## 쿠키 기반 Refresh Token

- httpOnly: JS 접근 차단 (XSS 방지)
- Secure: HTTPS만 전송
- SameSite=Lax: CSRF 기본 방지
- Path=/api/auth/refresh: 리프레시 엔드포인트에만 전송
