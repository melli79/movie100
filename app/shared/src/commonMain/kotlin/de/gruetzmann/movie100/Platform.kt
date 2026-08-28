package de.gruetzmann.movie100

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform