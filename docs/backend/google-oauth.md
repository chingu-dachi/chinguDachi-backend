# Google OAuth 연동 + AuthenticateUseCase

## 개요

Google OAuth 2.0 인증 흐름의 핵심 비즈니스 로직.
프로젝트 최초의 UseCase(application layer)로, Clean Architecture의 핵심 계층을 완성한다.

## 아키텍처

```
[Presentation]           [Application]              [Infrastructure]
                    AuthenticateUseCase
                         │    │
                 OAuthClient(port)──────► GoogleOAuthClient
                         │                     │
                         │               RestClient → Google API
                         │
                  AccountRepository
                  AccountCredentialRepository
                  AuthTokenRepository
                  JwtProvider
```

핵심: `OAuthClient`는 application 레이어의 포트 인터페이스.
Infrastructure의 `GoogleOAuthClient`가 구현 → DIP 준수.

## 인증 흐름

```
1. 프론트에서 Google 로그인 → authorization code 획득
2. POST /api/auth/google { code }
3. AuthenticateUseCase.authenticate(code)
   ├─ GoogleOAuthClient.authenticate(code)
   │   ├─ code → Google Token API → access_token
   │   └─ access_token → Google UserInfo API → sub, email
   ├─ findOrCreateAccount
   │   ├─ AccountCredentialRepository.findByCredentialTypeAndOauthKey()
   │   ├─ 기존 유저 → 기존 Account 반환
   │   └─ 신규 유저 → Account(NOT_CONSENT) + Credential 생성
   ├─ 기존 refresh token 전체 삭제 (단일 디바이스 정책)
   ├─ 새 TokenPair(access + refresh) 생성
   └─ AuthenticateResult 반환 (tokens + onboardingRequired)
```

## 주요 파일

### Application Layer

| 파일 | 역할 |
|------|------|
| `application/auth/AuthenticateUseCase.kt` | 인증 핵심 비즈니스 로직 |
| `application/auth/port/OAuthClient.kt` | OAuth 포트 인터페이스 (DIP) |
| `application/auth/command/AuthenticateCommand.kt` | 입력 DTO |
| `application/auth/result/AuthenticateResult.kt` | 출력 DTO |

### Infrastructure Layer

| 파일 | 역할 |
|------|------|
| `infrastructure/oauth/GoogleOAuthClient.kt` | OAuthClient 구현체 (RestClient) |
| `infrastructure/oauth/GoogleOAuthProperties.kt` | Google OAuth 설정 |
| `infrastructure/oauth/dto/GoogleTokenResponse.kt` | Google 토큰 응답 DTO |
| `infrastructure/oauth/dto/GoogleUserInfoResponse.kt` | Google 유저정보 응답 DTO |
| `infrastructure/persistence/account/AccountCredentialRepository.kt` | Credential JPA 조회 |
| `infrastructure/config/RestClientConfig.kt` | RestClient 빈 등록 |

## 설계 결정

| 결정 | 이유 |
|------|------|
| OAuthClient 포트 인터페이스 | application → infrastructure 직접 참조 금지 (DIP) |
| AuthenticateUseCase는 concrete class | 구현체 1개, 인터페이스 불필요 |
| RestClient 사용 | Spring Boot 4.x 권장 (RestTemplate deprecated) |
| 로그인 시 기존 refresh token 전체 삭제 | 단일 디바이스 정책 (MVP) |
| 신규 유저 상태 NOT_CONSENT | 약관 동의 → 온보딩 순서 |

## 에러 처리

| ErrorCode | HTTP | 상황 |
|-----------|------|------|
| `AUTH_OAUTH_CODE_INVALID` | 401 | Google 토큰 교환 실패 (잘못된 code) |
| `AUTH_OAUTH_PROVIDER_ERROR` | 502 | Google UserInfo API 통신 실패 |

## OAuth 설정

| 항목 | 로컬 | 테스트 |
|------|------|--------|
| client-id | 환경변수 `GOOGLE_CLIENT_ID` | test-client-id |
| client-secret | 환경변수 `GOOGLE_CLIENT_SECRET` | test-client-secret |
| redirect-uri | http://localhost:3000/auth/callback | 동일 |
| token-uri | https://oauth2.googleapis.com/token (기본값) | 동일 |
| user-info-uri | https://www.googleapis.com/oauth2/v3/userinfo (기본값) | 동일 |

## 테스트

| 테스트 | 유형 | 케이스 수 |
|--------|------|-----------|
| `AuthenticateUseCaseTest` | Unit (MockK) | 5 (신규/기존/온보딩/토큰삭제/실패) |
| `GoogleOAuthClientTest` | Unit (MockRestServiceServer) | 3 (정상/토큰실패/정보실패) |
| `AccountCredentialRepositoryTest` | 영속성 (Embedded PG) | 3 (저장/조회/미존재) |
