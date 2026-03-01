# Auth 도메인 + JWT 인프라

## 개요

인증 인프라의 기반 레이어. JWT 토큰 생성/검증과 Auth 도메인 모델을 구축하여
이후 OAuth 연동(BE-05), Security Filter(BE-06)의 토대를 마련한다.

## 아키텍처

```
[JwtProvider]                     [AuthTokenRepository]
     │                                    │
     ├─ createAccessToken(accountId)      ├─ findByAccountIdAndRefreshToken()
     ├─ createRefreshToken(accountId)     ├─ deleteByAccountId()
     ├─ validateToken(token) → Boolean    └─ deleteByExpiresAtBefore()
     └─ parseAccountId(token) → Long
              │
              ├─ 만료 → UnauthorizedException(AUTH_TOKEN_EXPIRED)
              └─ 위변조 → UnauthorizedException(AUTH_TOKEN_INVALID)
```

## 도메인 모델

| 클래스 | 유형 | 역할 |
|--------|------|------|
| `AuthToken` | Entity | 리프레시 토큰 저장 + 만료 판단 (`isExpired()`) |
| `TokenPair` | Value Object | access/refresh 토큰 쌍 |
| `OAuthUserInfo` | Value Object | OAuth 인증 후 사용자 정보 추상화 |

## JWT 설정

| 항목 | 로컬 | 테스트 |
|------|------|--------|
| access-token-expiry | 1h | 10s |
| refresh-token-expiry | 30d | 1m |
| secret | Base64 256bit | Base64 256bit (별도) |

- `JwtProperties`로 외부화 (`@ConfigurationProperties(prefix = "jwt")`)
- `JwtProvider`에서 `@EnableConfigurationProperties`로 등록

## 에러 처리

`ExpiredJwtException`과 기타 `JwtException`을 분리하여 클라이언트가 토큰 갱신 여부를 판단할 수 있게 함.

| jjwt 예외 | ErrorCode | 의미 |
|-----------|-----------|------|
| `ExpiredJwtException` | `AUTH_TOKEN_EXPIRED` | 토큰 만료 → refresh 시도 |
| `SignatureException`, `MalformedJwtException` | `AUTH_TOKEN_INVALID` | 위변조/형식 오류 |

기존 `ErrorResponseWrappingAdvice`가 `UnauthorizedException` → HTTP 401 + `ApiResponse.error()` 변환.

## 테스트

| 테스트 | 유형 | 케이스 수 |
|--------|------|-----------|
| `AuthTokenTest` | 순수 단위 | 2 (만료 판단) |
| `JwtProviderTest` | 순수 단위 | 12 (생성/검증/파싱/에러) |
| `AuthTokenRepositoryTest` | 영속성 (Embedded PG) | 5 (CRUD/쿼리) |

## 의존성

- `io.jsonwebtoken:jjwt-api:0.13.0` (compile)
- `io.jsonwebtoken:jjwt-impl:0.13.0` (runtime)
- `io.jsonwebtoken:jjwt-jackson:0.13.0` (runtime)
