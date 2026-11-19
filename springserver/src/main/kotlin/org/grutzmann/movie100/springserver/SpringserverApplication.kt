package org.grutzmann.movie100.springserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ulid.ULID
import kotlin.random.Random

@SpringBootApplication
class SpringserverApplication

@Configuration
class AppConfig {
    @Bean
    fun random() = Random(System.currentTimeMillis())

    @Bean
    fun ulidFactory(random :Random) = ULID.Factory(random)
}

fun main(args: Array<String>) {
    runApplication<SpringserverApplication>(*args)
}
