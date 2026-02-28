package com.chat.chingudachi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import java.util.TimeZone

@EnableConfigurationProperties
@SpringBootApplication
class ChinguDachiApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<ChinguDachiApplication>(*args)
}
