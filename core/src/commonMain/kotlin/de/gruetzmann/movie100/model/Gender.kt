package de.gruetzmann.movie100.model

import kotlinx.serialization.Serializable

@Serializable
enum class Gender(val symbol :String) {
    MALE("♂︎"), FEMALE("♀︎"), OTHER("-");
}
