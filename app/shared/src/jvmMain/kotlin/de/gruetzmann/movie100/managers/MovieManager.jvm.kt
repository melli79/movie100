package de.gruetzmann.movie100.managers

actual val serverUrl = "http://localhost:8080"
actual fun isOutOfMemoryError(e: Throwable): Boolean = e is OutOfMemoryError