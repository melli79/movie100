package de.gruetzmann.movie100.model

import kotlinx.serialization.Serializable
import ulid.ULID

@Serializable
data class UserDto(
    val id :ULID,
    var name :String,
    var login :String,
    var avatar :Url? =null,
) {

}
