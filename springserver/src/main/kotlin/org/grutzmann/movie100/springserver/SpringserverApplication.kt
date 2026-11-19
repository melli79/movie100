package org.grutzmann.movie100.springserver

import org.ehcache.core.EhcacheManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.co.conoregan.themoviedbapi.TmdbApi
import ulid.ULID
import kotlin.random.Random

@SpringBootApplication
class SpringserverApplication

@EnableCaching
@Configuration
class AppConfig {
    @Bean
    fun random() = Random(System.currentTimeMillis())

    @Bean
    fun ulidFactory(random :Random) = ULID.Factory(random)

    @Bean
    fun tmbdApi(@Value("\${movie100.api-key}") apiKey :String) = TmdbApi(apiKey)
}

fun main(args: Array<String>) {
    runApplication<SpringserverApplication>(*args)
}
