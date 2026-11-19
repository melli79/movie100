package de.gruetzmann.movie100.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    val login: String? =null,
    val user: String? =null,
    val username: String? = null,
    val password :String? =null,
)