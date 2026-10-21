package de.gruetzmann.movie100.db

import de.gruetzmann.movie100.model.Gender
import de.gruetzmann.movie100.model.inits.knownMovies
import de.gruetzmann.movie100.ulidFactory
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object UsersTable : Table("users") {
    val id = varchar("id", 26)
    val name = varchar("name", 255)
    val login = varchar("login", 40).uniqueIndex()
    val avatar = varchar("avatar", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

object MoviesTable : Table("movies") {
    val id = integer("id")
    val title = varchar("title", 255)
    val poster = varchar("poster", 1024).nullable()
    val director = varchar("director", 255).nullable()
    val releaseDate = ulong("release_date").nullable()
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ActorsTable : Table("actors") {
    val id = integer("id")
    val name = varchar("name", 255)
    val gender = enumeration<Gender>("gender")
    val birthyear = uinteger("birthyear").nullable()
    val specialty = varchar("specialty", 255).nullable()

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(name, birthyear)
    }
}

object MovieActorsTable : Table("movie_actors") {
    val movieId = integer("movie_id").references(MoviesTable.id, onDelete = ReferenceOption.CASCADE)
    val actorId = integer("actor_id").references(ActorsTable.id, onDelete = ReferenceOption.CASCADE)
    val orderIndex = integer("order_index").default(0)

    override val primaryKey = PrimaryKey(movieId, actorId)
    init {
        uniqueIndex(movieId, orderIndex)
    }
}

object UserMoviesTable : Table("user_movies") {
    val userId = varchar("user_id", 26).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val movieId = integer("movie_id").references(MoviesTable.id, onDelete = ReferenceOption.CASCADE)
    val orderIndex = integer("order_index").default(0)

    override val primaryKey = PrimaryKey(userId, movieId)
    init {
        uniqueIndex(userId, orderIndex)
    }
}

object DatabaseFactory {
    fun init(dbUrl: String = "jdbc:sqlite:movie100.db") {
        val database = Database.connect(dbUrl, driver= "org.sqlite.JDBC")
        transaction(database) {
            exec("PRAGMA foreign_keys = ON;", explicitStatementType= StatementType.OTHER)
            SchemaUtils.create(
                UsersTable,
                MoviesTable,
                ActorsTable,
                MovieActorsTable,
                UserMoviesTable
            )

            if (UsersTable.selectAll().count() == 0L) {
                val mellisId = ulidFactory.nextULID().toString()
                UsersTable.insert {
                    it[id] = mellisId
                    it[name] = "M.Gr."
                    it[login] = "melli"
                    it[avatar] = null
                }

                knownMovies.forEachIndexed { movieIndex, movie ->
                    MoviesTable.insertIgnore {
                        it[id] = movie.id
                        it[title] = movie.title
                        it[poster] = movie.poster?.location
                        it[director] = movie.director
                        it[releaseDate] = movie.releaseDate?.epochSeconds?.toULong()
                        it[description] = movie.description
                    }

                    movie.actors.forEachIndexed { actorIndex, actor ->
                        ActorsTable.insertIgnore {
                            it[id] = actor.id
                            it[name] = actor.name
                            it[gender] = actor.gender
                            it[birthyear] = actor.birthyear?.toUInt()
                            it[specialty] = actor.specialty
                        }

                        MovieActorsTable.insertIgnore {
                            it[movieId] = movie.id
                            it[actorId] = actor.id
                            it[orderIndex] = actorIndex
                        }
                    }

                    UserMoviesTable.insertIgnore {
                        it[userId] = mellisId
                        it[movieId] = movie.id
                        it[orderIndex] = movieIndex
                    }
                }
            }
        }
    }
}
