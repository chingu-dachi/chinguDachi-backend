# 프로필 이미지 업로드

> **현재 상태: Stub** — S3 연동 전까지 검증만 수행하고 `"DEFAULT"` URL을 반환합니다.
> 실제 파일 저장은 S3 연동 시 구현 예정.

## 엔드포인트
POST /api/uploads/profile-image

## 인증
필요 (JWT Bearer token)

## 요청

### Headers
| 헤더 | 값 |
|------|-----|
| Authorization | Bearer {accessToken} |
| Content-Type | multipart/form-data |

### Body (multipart/form-data)
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| file | File | Y | 프로필 이미지 파일 |

### 제약 조건
- **파일 크기**: 최대 5MB
- **허용 포맷**: JPEG, PNG, WebP (`image/jpeg`, `image/png`, `image/webp`)

## 응답

### 성공 (200 OK) — Stub
```json
{
  "code": null,
  "data": {
    "imageUrl": "DEFAULT"
  },
  "message": "success",
  "isSuccess": true,
  "timestamp": "2026-03-02T10:00:00Z"
}
```

### 에러 케이스

| 상태 | 에러 코드 | 설명 |
|------|-----------|------|
| 400 | UPLOAD_FILE_TOO_LARGE | 파일 크기 5MB 초과 |
| 400 | UPLOAD_INVALID_FORMAT | JPEG/PNG/WebP 외 포맷 |
| 401 | UNAUTHORIZED | JWT 없음 또는 유효하지 않음 |

## 구현 상세

### 현재 동작 (Stub)
- 파일 크기, 포맷 검증만 수행
- 검증 통과 시 `"DEFAULT"` 문자열 반환 (실제 저장 없음)
- `FileStore.saveProfileImage()`는 호출하지 않음

### 아키텍처
- `FileStore` 포트 인터페이스 → `LocalFileStoreAdapter` 구현체 존재
- S3 연동 시 `UploadProfileImageUseCase`에서 `fileStore.saveProfileImage()` 호출로 교체
- 리사이즈 (400x400) 등 이미지 처리도 S3 연동 시 함께 구현 예정
