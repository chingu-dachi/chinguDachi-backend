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
    ACCOUNT_NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "Nickname is already taken"),
    ACCOUNT_ALREADY_ONBOARDED(HttpStatus.CONFLICT, "Onboarding is already completed"),
    ACCOUNT_ALREADY_DELETED(HttpStatus.CONFLICT, "Account is already deleted"),

    // Upload
    UPLOAD_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "File size must be 5MB or less"),
    UPLOAD_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WebP formats are allowed"),

    // Interest
    INTEREST_TAG_NOT_FOUND(HttpStatus.BAD_REQUEST, "One or more interest tags not found"),
    INTEREST_REQUIRED(HttpStatus.BAD_REQUEST, "At least one interest is required"),

    // Auth
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Invalid token"),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh token has expired"),
    AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    AUTH_OAUTH_REDIRECT_URI_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Redirect URI is not allowed"),
    AUTH_OAUTH_CODE_INVALID(HttpStatus.UNAUTHORIZED, "Invalid OAuth authorization code"),
    AUTH_OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAuth provider communication failed"),

    // Chat
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat room not found"),
    CHAT_ROOM_SELF_CHAT(HttpStatus.BAD_REQUEST, "Cannot create chat room with yourself"),
    CHAT_ROOM_TARGET_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "Target user is not active"),
    CHAT_NOT_A_MEMBER(HttpStatus.FORBIDDEN, "Not a member of this chat room"),
    CHAT_ROOM_GROUP_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "Group chat is not supported yet"),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "Message must not be empty"),
    CHAT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "Message must be 1000 characters or less"),
    CHAT_ROOM_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "Chat room is not active"),

    // Translation
    TRANSLATION_API_ERROR(HttpStatus.BAD_GATEWAY, "Translation API communication failed"),
    TRANSLATION_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Translation API timed out"),
    TRANSLATION_ALL_MODELS_FAILED(HttpStatus.BAD_GATEWAY, "All translation models failed"),
}
