package com.chat.chingudachi.domain.common

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Invalid input"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid parameter"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Permission denied"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),
    CONFLICT(HttpStatus.CONFLICT, "Resource conflict"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected system error has occurred"),

    // Account
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Account not found"),
    ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Account already exists"),
    ACCOUNT_NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "Nickname must be 2-12 characters without spaces"),

    // Auth
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Invalid token"),
    AUTH_OAUTH_CODE_INVALID(HttpStatus.UNAUTHORIZED, "Invalid OAuth authorization code"),
    AUTH_OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAuth provider communication failed"),
}
