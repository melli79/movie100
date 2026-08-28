package de.gruetzmann.movie100

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Movie100",
    ) {
        App()
    }
}