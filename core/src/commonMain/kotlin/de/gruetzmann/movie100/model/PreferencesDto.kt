package de.gruetzmann.movie100.model

import kotlinx.serialization.Serializable
import ulid.ULID

@Serializable
data class PreferencesDto(val userId :ULID, val movies :MutableList<MovieDto> =mutableListOf()) {

}
