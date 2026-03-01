# 온보딩 완료

## 엔드포인트
PUT /api/users/profile

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
| birthDate | String | Y | 생년월일 (YYYY-MM-DD) |
| nation | String | Y | 국적 (KR, JP) |
| interestTagIds | Long[] | Y | 관심사 태그 ID 목록 (1개 이상) |

### 요청 예시
```json
{
  "nickname": "테스트유저",
  "birthDate": "2000-01-01",
  "nation": "KR",
  "interestTagIds": [1, 3, 5]
}
```

## 비즈니스 규칙
- `nativeLanguage`는 `nation`에서 자동 파생 (KR → KO, JP → JA)
- 기존 관심사를 전체 삭제 후 새로 저장 (replace 전략)
- 온보딩 완료 시 accountStatus: NOT_CONSENT → ACTIVE

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
| 400 | INTEREST_REQUIRED | 관심사를 1개 이상 선택해야 함 |
| 400 | INTEREST_TAG_NOT_FOUND | 존재하지 않는 관심사 태그 ID |
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |
| 404 | ACCOUNT_NOT_FOUND | 계정을 찾을 수 없음 |
| 409 | ACCOUNT_NICKNAME_DUPLICATE | 닉네임이 이미 사용 중 |
