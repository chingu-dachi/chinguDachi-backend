package com.chat.chingudachi.domain.common

sealed class BusinessException(
    val errorCode: ErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)

class BadRequestException(
    errorCode: ErrorCode = ErrorCode.INVALID_INPUT,
    cause: Throwable? = null,
) : BusinessException(errorCode, cause)

class UnauthorizedException(
    errorCode: ErrorCode = ErrorCode.UNAUTHORIZED,
    cause: Throwable? = null,
) : BusinessException(errorCode, cause)

class ForbiddenException(
    errorCode: ErrorCode = ErrorCode.FORBIDDEN,
    cause: Throwable? = null,
) : BusinessException(errorCode, cause)

class NotFoundException(
    errorCode: ErrorCode = ErrorCode.RESOURCE_NOT_FOUND,
    cause: Throwable? = null,
) : BusinessException(errorCode, cause)

class ConflictException(
    errorCode: ErrorCode = ErrorCode.CONFLICT,
    cause: Throwable? = null,
) : BusinessException(errorCode, cause)

class InternalServerException(
    errorCode: ErrorCode = ErrorCode.INTERNAL_ERROR,
    cause: Throwable? = null,
) : BusinessException(errorCode, cause)
