package de.gruetzmann.movie100.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import de.gruetzmann.movie100.model.Url
import ulid.ULID

data class Movie(
    val id :Int,
    val title :String,
    val actors :List<String>,
    var inceptionYear :Int? =null,
    val director :String? =null,
    val posterUrl :Url? =null,
    var poster :ImageBitmap? =null,
    val details :String? =null,
) {
    override fun toString() = "$title *$inceptionYear ${if (director!=null) "by $director" else ""} with $actors"
}
