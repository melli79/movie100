package de.gruetzmann.movie100.managers

actual val serverUrl = "http://10.0.2.2:8080"
actual fun isOutOfMemoryError(e: Throwable): Boolean = e is OutOfMemoryError