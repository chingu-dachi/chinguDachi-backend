package com.chat.chingudachi.account.application.command

data class AgreeToTermsCommand(
    val termsOfServiceAgree: Boolean,
    val privacyPolicyAgree: Boolean,
    val crossBorderDataAgree: Boolean,
    val marketingAgree: Boolean,
)
