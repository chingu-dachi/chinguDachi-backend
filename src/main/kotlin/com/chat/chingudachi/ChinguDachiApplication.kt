package com.chat.chingudachi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.util.TimeZone

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableJpaAuditing
class ChinguDachiApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<ChinguDachiApplication>(*args)
}
