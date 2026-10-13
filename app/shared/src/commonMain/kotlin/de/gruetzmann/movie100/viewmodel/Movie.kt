package de.gruetzmann.movie100.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import ulid.ULID

data class Movie(
    val id :ULID,
    val name :String,
    val actors :List<String>,
    var inceptionYear :Int = 2010,
    val director :String? =null,
    val poster :ImageBitmap? =null,
    val details :String? =null,
) {
    override fun toString() = "$name $inceptionYear ${if (director!=null) "by $director" else ""} with $actors"
}
