# 프로필 이미지 업로드

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
- **허용 포맷**: JPEG, PNG, WebP

## 응답

### 성공 (200 OK)
```json
{
  "code": null,
  "data": {
    "imageUrl": "http://localhost:8080/uploads/profile-images/550e8400-e29b-41d4-a716-446655440000.jpg"
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

### 저장 방식
- 로컬: `uploads/profile-images/` 디렉토리에 UUID 파일명으로 저장
- 파일 서빙: `GET /uploads/profile-images/{filename}` (인증 불필요)

### 아키텍처
- `FileStore` 포트 → `LocalFileStoreAdapter` (로컬)
- 추후 S3 등 클라우드 스토리지로 교체 시 어댑터만 변경
