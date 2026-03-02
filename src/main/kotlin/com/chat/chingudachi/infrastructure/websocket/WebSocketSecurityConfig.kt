package com.chat.chingudachi.infrastructure.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
class WebSocketSecurityConfig(
    private val stompAuthChannelInterceptor: StompAuthChannelInterceptor,
) : WebSocketMessageBrokerConfigurer {
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompAuthChannelInterceptor)
    }
}
