package com.chat.chingudachi.domain.common

enum class ErrorCode(val message: String) {
    // Common
    INVALID_INPUT("Invalid input"),
    INVALID_PARAMETER("Invalid parameter"),
    UNAUTHORIZED("Authentication required"),
    FORBIDDEN("Permission denied"),
    RESOURCE_NOT_FOUND("Resource not found"),
    METHOD_NOT_ALLOWED("Method not allowed"),
    CONFLICT("Resource conflict"),
    INTERNAL_ERROR("An unexpected system error has occurred"),

    // Account
    ACCOUNT_NOT_FOUND("Account not found"),
    ACCOUNT_ALREADY_EXISTS("Account already exists"),
    ACCOUNT_NICKNAME_INVALID("Nickname must be 2-12 characters without spaces"),
    ACCOUNT_NICKNAME_DUPLICATE("Nickname is already taken"),
    ACCOUNT_ALREADY_ONBOARDED("Onboarding is already completed"),
    ACCOUNT_ALREADY_DELETED("Account is already deleted"),

    // Upload
    UPLOAD_FILE_TOO_LARGE("File size must be 5MB or less"),
    UPLOAD_INVALID_FORMAT("Only JPEG, PNG, WebP formats are allowed"),

    // Interest
    INTEREST_TAG_NOT_FOUND("One or more interest tags not found"),
    INTEREST_REQUIRED("At least one interest is required"),

    // Auth
    AUTH_TOKEN_EXPIRED("Token has expired"),
    AUTH_TOKEN_INVALID("Invalid token"),
    AUTH_REFRESH_TOKEN_EXPIRED("Refresh token has expired"),
    AUTH_REFRESH_TOKEN_INVALID("Invalid refresh token"),
    AUTH_OAUTH_REDIRECT_URI_NOT_ALLOWED("Redirect URI is not allowed"),
    AUTH_OAUTH_CODE_INVALID("Invalid OAuth authorization code"),
    AUTH_OAUTH_PROVIDER_ERROR("OAuth provider communication failed"),

    // Chat
    CHAT_ROOM_NOT_FOUND("Chat room not found"),
    CHAT_ROOM_SELF_CHAT("Cannot create chat room with yourself"),
    CHAT_ROOM_TARGET_NOT_ACTIVE("Target user is not active"),
    CHAT_NOT_A_MEMBER("Not a member of this chat room"),
    CHAT_ROOM_GROUP_NOT_SUPPORTED("Group chat is not supported yet"),
    CHAT_MESSAGE_EMPTY("Message must not be empty"),
    CHAT_MESSAGE_TOO_LONG("Message must be 1000 characters or less"),
    CHAT_ROOM_NOT_ACTIVE("Chat room is not active"),
    CHAT_MESSAGE_NOT_FOUND("Chat message not found"),
    CHAT_MESSAGE_ROOM_MISMATCH("Message does not belong to this chat room"),
    CHAT_TRANSLATION_NOT_FAILED("Translation retry is only available for failed translations"),
    CHAT_CANNOT_RETRY_OWN_MESSAGE("Cannot retry translation on your own message"),

    // Translation
    TRANSLATION_API_ERROR("Translation API communication failed"),
    TRANSLATION_TIMEOUT("Translation API timed out"),
    TRANSLATION_RETRY_EXHAUSTED("Translation failed after all retries"),
}
