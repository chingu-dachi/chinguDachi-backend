package com.chat.chingudachi.infrastructure.config

import com.chat.chingudachi.presentation.common.AuthAccountIdArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authAccountIdArgumentResolver: AuthAccountIdArgumentResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authAccountIdArgumentResolver)
    }
}
