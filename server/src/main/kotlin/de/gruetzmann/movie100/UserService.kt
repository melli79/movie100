package de.gruetzmann.movie100

import de.gruetzmann.movie100.db.*
import de.gruetzmann.movie100.model.*
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ulid.ULID
import kotlin.time.Instant

object UserService {

    fun login(login :String) :UserDto = transaction {
        val existingUserRow = UsersTable.selectAll()
            .firstOrNull { it[UsersTable.login].equals(login, ignoreCase = true) }

        return@transaction if (existingUserRow != null)
            UserDto(
                id = ULID.parseULID(existingUserRow[UsersTable.id]),
                name = existingUserRow[UsersTable.name],
                login = existingUserRow[UsersTable.login],
                avatar = existingUserRow[UsersTable.avatar]?.let { Url(it) }
            )
        else {
            val newUlid = ulidFactory.nextULID()
            UsersTable.insert {
                it[id] = newUlid.toString()
                it[name] = login
                it[UsersTable.login] = login
                it[avatar] = null
            }
            UserDto(
                id = newUlid,
                name = login,
                login = login,
            )
        }

    }

    fun getPreferences(user :UserDto) :PreferencesDto = transaction {
        val userExists = UsersTable.selectAll()
            .where { UsersTable.id eq user.id.toString() }
            .count() > 0
        if (!userExists) {
            throw NotFoundException("Preferences not found. Did you login?")
        }

        val movieRows = (UserMoviesTable innerJoin MoviesTable)
            .selectAll()
            .where { UserMoviesTable.userId eq user.id.toString() }
            .orderBy(UserMoviesTable.orderIndex to SortOrder.ASC)

        val movies = movieRows.map { movieRow ->
            val movieId = movieRow[MoviesTable.id]
            val actorRows = (MovieActorsTable innerJoin ActorsTable)
                .selectAll()
                .where { MovieActorsTable.movieId eq movieId }
                .orderBy(MovieActorsTable.orderIndex to SortOrder.ASC)

            val actors = actorRows.map { actorRow ->
                ActorDto(
                    id= actorRow[ActorsTable.id],
                    name= actorRow[ActorsTable.name],
                    gender= actorRow[ActorsTable.gender],
                    birthyear= actorRow[ActorsTable.birthyear]?.toInt(),
                    specialty= actorRow[ActorsTable.specialty]
                )
            }.toMutableList()

            MovieDto(
                id= movieId,
                title= movieRow[MoviesTable.title],
                poster= movieRow[MoviesTable.poster]?.let { Url(it) },
                actors= actors,
                director= movieRow[MoviesTable.director],
                releaseDate= movieRow[MoviesTable.releaseDate]?.let { Instant.fromEpochSeconds(it.toLong()) },
                description= movieRow[MoviesTable.description]
            )
        }.toMutableList()

        PreferencesDto(user.id, movies)
    }

    fun savePreferences(preferences: PreferencesDto): PreferencesDto = transaction {
        val userId = preferences.userId.toString()
        println("User id: $userId")
        val userExists = UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .count()
        println("User exists: $userExists")
        if (userExists<=0) {
            throw NotFoundException("User not found. Did you login?")
        }

        for (movie in preferences.movies) {
            if (movie.actors.size > 5) {
                movie.actors.dropLast(movie.actors.size - 5)
            }

            MoviesTable.upsert {
                it[id] = movie.id
                it[title] = movie.title
                it[poster] = movie.poster?.location
                it[director] = movie.director
                it[releaseDate] = movie.releaseDate?.epochSeconds?.toULong()
                it[description] = movie.description
            }

            for ((actorIndex, actor) in movie.actors.withIndex()) {
                ActorsTable.upsert {
                    it[id] = actor.id
                    it[name] = actor.name
                    it[gender] = actor.gender
                    it[birthyear] = actor.birthyear?.toUInt()
                    it[specialty] = actor.specialty
                }

                MovieActorsTable.upsert {
                    it[movieId] = movie.id
                    it[actorId] = actor.id
                    it[orderIndex] = actorIndex
                }
            }
        }

        UserMoviesTable.deleteWhere { this.userId eq userId }
        preferences.movies.forEachIndexed { movieIndex, movie ->
            UserMoviesTable.insert {
                it[this.userId] = userId
                it[movieId] = movie.id
                it[orderIndex] = movieIndex
            }
        }

        preferences
    }

    fun deleteUser(login :String) :Boolean = transaction {
        val userRow = UsersTable.selectAll()
            .firstOrNull { it[UsersTable.login].equals(login, ignoreCase = true) }
            ?: return@transaction false

        val userId = userRow[UsersTable.id]
        UserMoviesTable.deleteWhere { this.userId eq userId }
        UsersTable.deleteWhere { id eq userId }

        true
    }
}
