# Logging Architecture

## 개요

MDC(Mapped Diagnostic Context) 기반 요청 추적 로깅 시스템.
모든 HTTP 요청에 고유 `requestId`를 부여하여 로그 흐름을 추적한다.

## 아키텍처

```
HTTP Request
    │
    ▼
MdcLoggingFilter (OncePerRequestFilter)
    ├── MDC.put(requestId, method, uri, clientIp, userId, ...)
    ├── filterChain.doFilter()
    ├── 요약 로그: "GET /api/users 200 23ms"
    └── MDC.clear()
    │
    ▼
logback-spring.xml
    ├── local: 컬러 + DEBUG + 짧은 타임스탬프
    └── prod:  플레인 + INFO + 풀 타임스탬프
```

## MDC 필드

| 필드 | 설명 | 로그 패턴 포함 | 비고 |
|------|------|:-:|------|
| `requestId` | UUID 앞 8자리 | O | 요청 추적 식별자 |
| `method` | HTTP 메서드 | X | 요약 로그에 출력 |
| `uri` | 요청 URI | X | 요약 로그에 출력 |
| `clientIp` | 클라이언트 IP | X | X-Forwarded-For 우선 |
| `userId` | 사용자 ID | X | 현재 `anonymous` 고정 |
| `userAgent` | User-Agent 헤더 | X | 에러 진단용 |
| `origin` | Origin 헤더 | X | 에러 진단용 |

## 로그 출력 예시

### local 프로필

```
17:30:11.591 INFO  [a1b2c3d4] c.c.c.filter.MdcLoggingFilter        : GET /api/accounts 200 23ms
17:30:11.595 WARN  [a1b2c3d4] c.c.c.p.c.ErrorResponseWrappingAdvice : Business exception: ACCOUNT_NOT_FOUND - Account not found
```

### prod 프로필 (default)

```
2026-03-01 17:30:11.591 INFO  [a1b2c3d4] [http-nio-8080-exec-1] c.c.c.filter.MdcLoggingFilter : GET /api/accounts 200 23ms
```

## 프로필별 설정

| 항목 | local | prod (default) |
|------|-------|----------------|
| 로그 레벨 | DEBUG | INFO |
| 타임스탬프 | `HH:mm:ss.SSS` | `yyyy-MM-dd HH:mm:ss.SSS` |
| 컬러 | O | X |
| 스레드명 | X | O |

## kotlin-logging 사용법

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

// 파일 최상단 (클래스 밖)
private val log = KotlinLogging.logger {}

// 사용
log.info { "메시지: $value" }                    // 람다 (lazy)
log.error(exception) { "에러 메시지: $detail" }  // 예외 포함
log.debug { "디버그: ${expensive()}" }           // 레벨 비활성 시 평가 안 함
```

## 향후 확장

| 단계 | 도입 | 용도 |
|------|------|------|
| 1 | Zalando Logbook | 요청/응답 Body 로깅 + 필드 마스킹 |
| 2 | logstash-logback-encoder | JSON 구조화 로그 (ELK/CloudWatch) |
| 3 | OpenTelemetry | 분산 추적 (MSA 전환 시) |
| 4 | Actuator + Micrometer | 메트릭 수집 + Prometheus 연동 |
