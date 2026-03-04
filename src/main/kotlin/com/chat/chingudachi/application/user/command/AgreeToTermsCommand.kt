package com.chat.chingudachi.application.user.command

data class AgreeToTermsCommand(
    val termsOfServiceAgree: Boolean,
    val privacyPolicyAgree: Boolean,
    val crossBorderDataAgree: Boolean,
    val marketingAgree: Boolean,
)
