package de.gruetzmann.movie100.model

import kotlinx.serialization.Serializable
import ulid.ULID
import kotlin.time.Clock.System.now
import kotlin.time.Instant

@Serializable
data class MovieDto(
    val id :Int,
    var title :String,
    var poster :Url? =null,
    val actors :MutableList<ActorDto> =mutableListOf(),
    var director :String? =null,
    @Serializable(with = InstantSerializer::class)
    var releaseDate :Instant? =null,
    var description :String? =null,
) {
}

@Serializable
data class ActorDto(
    val id :Int,
    var name :String,
    var gender :Gender,
    var birthyear :Int? =null,
    var specialty :String? =null,
) {
    override fun toString() = "$name ${gender.symbol} ${if (birthyear!=null) "($age)" else ""}"

    val age :Int?
        get() = if (birthyear!=null) now().years() - birthyear!!  else null
}

fun Instant.years() = (epochSeconds/3600/24/365.25).toInt()+1970
