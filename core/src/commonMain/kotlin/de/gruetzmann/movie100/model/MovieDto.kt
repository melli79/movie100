package de.gruetzmann.movie100.model

import ulid.ULID
import kotlin.time.Clock.System.now
import kotlin.time.Instant

data class MovieDto(
    val id :ULID,
    var name :String,
    var poster :Url? =null,
    val actors :MutableList<ActorDto> =mutableListOf(),
    var director :String? =null,
    var releaseDate :Instant =Instant.DISTANT_PAST,
    var description :String? =null,
) {
}

data class ActorDto(
    val id :ULID,
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
