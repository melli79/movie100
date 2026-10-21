package de.gruetzmann.movie100

import de.gruetzmann.movie100.model.ActorDto
import de.gruetzmann.movie100.model.Gender
import de.gruetzmann.movie100.model.MovieDto
import de.gruetzmann.movie100.model.PreferencesDto
import de.gruetzmann.movie100.model.Url
import de.gruetzmann.movie100.model.UserDto
import de.gruetzmann.movie100.model.inits.knownMovies
import io.ktor.server.plugins.NotFoundException
import ulid.ULID
import kotlin.random.Random
import kotlin.time.Instant

val random = Random(System.currentTimeMillis())

object UserService {
    private val ulidFactory = ULID.Factory(random)
    val knownUser = UserDto(ulidFactory.nextULID(), "M.Gr.", "melli")
    private val users = mutableListOf<UserDto>(knownUser)
    private val preferences = mutableMapOf<ULID, PreferencesDto>(knownUser.id to PreferencesDto(knownUser.id,
        knownMovies.toMutableList()))

    fun login(login :String) :UserDto {
        val candidate = users.firstOrNull { it.login.equals(login, true) }
        if (candidate!=null)
            return candidate
        val newUser = UserDto(ulidFactory.nextULID(), login, login)
        users.add(newUser)
        preferences.computeIfAbsent(newUser.id) { PreferencesDto(newUser.id) }
        return newUser
    }

    fun getPreferences(user :UserDto) :PreferencesDto = preferences.getOrElse(user.id) {
        throw NotFoundException("Preferences not found.  Did you login?")
    }

    fun savePreferences(preferences :PreferencesDto) :PreferencesDto {
        val userId = preferences.userId
        val user = users.firstOrNull { it.id == userId } ?: throw NotFoundException("User not found. Did you login?")
        for ((_, _, _, actors) in preferences.movies) {
            if (actors.size>5) actors.dropLast(actors.size-5)
        }
        this.preferences[userId] = preferences
        return preferences
    }

    fun deleteUser(login :String) :Boolean {
        val user = users.firstOrNull { it.login.equals(login, true) } ?: return false
        preferences.remove(user.id)
        return users.remove(user)
    }
}
