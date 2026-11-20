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
                releaseDate= movieRow[MoviesTable.releaseDate]?.let { Instant.fromEpochSeconds(it) },
                description= movieRow[MoviesTable.description]
            )
        }.toMutableList()

        PreferencesDto(user.id, movies)
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

    fun findUser(login :String) :UserDto? = transaction {
        val rs = UsersTable.selectAll()
            .where { UsersTable.login eq login }
        if (rs.count()<1)
            return@transaction null
        val result :ResultRow = rs.first()
        return@transaction UserDto(ULID.parseULID(result.get(UsersTable.id)), result.get(UsersTable.name),
            result.get(UsersTable.login), result.get(UsersTable.avatar)?.let { Url(it) })
    }
}
