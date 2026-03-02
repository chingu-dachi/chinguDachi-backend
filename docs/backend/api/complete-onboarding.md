# 온보딩 완료

## 엔드포인트
PUT /api/users/me

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |
| Content-Type | application/json |

### Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| nickname | String | Y | 닉네임 (2~12자, 공백 불가) |
| birthYear | Int | Y | 출생연도 |
| nationality | String | Y | 국적 코드 (KR, JP) |
| interests | String[] | Y | 관심사 태그 키 목록 (1개 이상) |
| bio | String? | N | 자기소개 |
| city | String? | N | 도시 |
| profileImageUrl | String? | N | 프로필 이미지 URL |

### 요청 예시
```json
{
  "nickname": "테스트유저",
  "birthYear": 2000,
  "nationality": "KR",
  "interests": ["TRAVEL", "FOOD"],
  "bio": "안녕하세요",
  "city": "Seoul",
  "profileImageUrl": null
}
```

### 변경 이력 (BE-28)

| 이전 | 이후 | 설명 |
|------|------|------|
| PUT /api/users/profile | PUT /api/users/me | 경로 변경 |
| birthDate (LocalDate) | birthYear (Int) | 연도만 수신 |
| nation (enum) | nationality (String) | 문자열로 변경 |
| interestTagIds (Long[]) | interests (String[]) | tagKey 기반 |
| - | bio, city, profileImageUrl | 신규 필드 |

## 비즈니스 규칙
- `translateLanguage`는 `nationality`에서 자동 파생 (KR → ja, JP → ko)
- 기존 관심사를 전체 삭제 후 새로 저장 (replace 전략)
- 온보딩 완료 시 accountStatus: NOT_CONSENT → ACTIVE
- `birthYear`는 내부적으로 `LocalDate.of(birthYear, 1, 1)`로 저장

## 응답

### 성공 (200 OK)
```json
{
  "code": null,
  "data": null,
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 400 | INVALID_INPUT | nationality 값이 유효하지 않음 |
| 400 | INTEREST_REQUIRED | 관심사를 1개 이상 선택해야 함 |
| 400 | INTEREST_TAG_NOT_FOUND | 존재하지 않는 관심사 태그 키 |
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
| 404 | ACCOUNT_NOT_FOUND | 계정을 찾을 수 없음 |
| 409 | ACCOUNT_ALREADY_ONBOARDED | 이미 온보딩이 완료된 계정 |
| 409 | ACCOUNT_NICKNAME_DUPLICATE | 닉네임이 이미 사용 중 |
