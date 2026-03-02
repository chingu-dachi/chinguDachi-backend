# 회원 탈퇴 + 로그아웃 PR 리뷰 대응

> PR #56 `feat(auth): 로그아웃 + 회원탈퇴 API` — 4인 패널 코드 리뷰 대응 결정 기록

## 리뷰 대응 결정

| # | 리뷰 포인트 | 결정 | 사유 |
|---|------------|------|------|
| 1 | SecurityConfig OCP (@PublicEndpoint) | SKIP | Spring 공식 문서가 중앙 집중 방식 권장. 커스텀 어노테이션은 디버깅 복잡도 증가 |
| 2 | clearRefreshTokenCookie() 중복 | **수용** | CookieUtils.kt top-level 함수로 추출 |
| 3 | Soft-delete 재로그인 차단 | SKIP | 재가입 허용 정책 (soft-delete 유저는 새 유저로 재가입) |
| 4 | DeleteAccountUseCase 트랜잭션 경계 | 현행 유지 | JPA 기반이라 @Transactional로 충분. 별도 경계 불필요 |
| 5 | Event 기반 분리 | 안 함 | 정합성 보장 + 디버깅 추적 어려움. 현재 규모에서 과도한 설계 |
| 6 | UserInterest soft-delete | **별도 이슈** | 지표 분석 + 재가입 시 관심사 복원 UX 가치. GitHub Issue로 분리 |
| 7 | 토큰 삭제 순서 | **수용** | authTokenStore.deleteByAccountId를 마지막으로 이동 (향후 외부 저장소 전환 대비) |
| 8 | 회원 탈퇴 로그 | **수용** | `log.info { "Account deleted: accountId=$accountId" }` 추가 |
| 9 | 탈퇴 사유 필드 | TODO | 다음 MVP에서 추가 예정 |

## 코드 변경 요약

### CookieUtils 추출 (#2)
- `presentation/common/CookieUtils.kt` — Kotlin top-level 함수 2개
  - `addRefreshTokenCookie(response, refreshToken, maxAge)`
  - `clearRefreshTokenCookie(response)`
- AuthController, UserController에서 private 메서드 제거 → import로 전환

### 토큰 삭제 순서 변경 (#7)
- `DeleteAccountUseCase.execute()` 내부 순서:
  1. `account.softDelete()` + `accountStore.save()`
  2. `userInterestStore.deleteByAccountId()` — 비즈니스 데이터 먼저
  3. `authTokenStore.deleteByAccountId()` — 인증 토큰 마지막 (외부 저장소 전환 대비)

### 로그 추가 (#8)
- `DeleteAccountUseCase`: `log.info` — 탈퇴는 중요 이벤트이므로 INFO
- `LogoutUseCase`: `log.debug` — 로그아웃은 일상적 동작이므로 DEBUG

## 별도 작업

- **GitHub Issue**: UserInterest soft-delete 전환 (지표 분석 + 재가입 UX)
- **다음 MVP TODO**: 탈퇴 사유 필드 추가

## 관련 문서

- [회원 탈퇴 API](./delete-account.md)
- [로그아웃 API](./logout.md)
