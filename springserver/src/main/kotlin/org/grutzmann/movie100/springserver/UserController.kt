package org.grutzmann.movie100.springserver

import de.gruetzmann.movie100.model.ErrorDto
import de.gruetzmann.movie100.model.LoginDto
import de.gruetzmann.movie100.model.PreferencesDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class UserController(
    private val userService: UserService,
    private val movieService :MovieService,
) {

    @PostMapping("/login/{user}")
    fun loginWithPath(
        @PathVariable("user") user: String,
        @RequestBody(required = true) body:LoginDto
    ): ResponseEntity<*> {
        if (user.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorDto("Missing login"))
        }
        val password = body.password
        if (password.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorDto("Insufficient credentials"))
        }
        val preferences = userService.login(user, password)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(preferences)
    }

    @PostMapping("/login")
    fun login(
        @RequestParam(name = "user", required = false) userParam: String?,
        @RequestParam(name = "login", required = false) loginParam: String?,
        @RequestBody(required = true) body:LoginDto
    ): ResponseEntity<*> {
        val username = userParam ?: loginParam ?: body.login ?: body.user ?: body.username
        if (username.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorDto("Missing login"))
        }
        val password = body.password
        if (password.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorDto("Insufficient credentials"))
        }
        val preferences = userService.login(username, password)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(preferences)
    }

    @PutMapping("/preferences/{login}")
    fun putUserPreferences(
        @PathVariable("login") login :String,
        @RequestBody(required= true) preferences :PreferencesDto
    ) :ResponseEntity<*> {
        val user = userService.findUser(login) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorDto("Cannot find user $login"))
        if (preferences.userId!=user.id)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorDto("userId does not match"))
        val saved = movieService.savePreferences(preferences)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(saved)
    }
}
