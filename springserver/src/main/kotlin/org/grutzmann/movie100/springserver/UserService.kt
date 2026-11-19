package org.grutzmann.movie100.springserver

import de.gruetzmann.movie100.model.PreferencesDto
import de.gruetzmann.movie100.model.Url
import de.gruetzmann.movie100.model.UserDto
import de.gruetzmann.movie100.model.inits.knownMovies
import jakarta.annotation.PostConstruct
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import ulid.ULID

@Service
class UserService(
    private val jdbcTemplate :JdbcTemplate,
    private val movieService :MovieService,
    private val ulidFactory :ULID.Factory
) {

    @PostConstruct
    fun initDatabase() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(26) NOT NULL PRIMARY KEY,
                name VARCHAR(255),
                login VARCHAR(40) UNIQUE,
                avatar VARCHAR(255)
            );
            """.trimIndent()
        )
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS movies (
                id INTEGER NOT NULL PRIMARY KEY,
                title VARCHAR(255),
                poster VARCHAR(1024),
                director VARCHAR(255),
                release_date BIGINT,
                description TEXT
            );
            """.trimIndent()
        )
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS actors (
                id INTEGER NOT NULL PRIMARY KEY,
                name VARCHAR(255),
                gender INTEGER,
                birthyear INTEGER,
                specialty VARCHAR(255)
            );
            """.trimIndent()
        )
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS movie_actors (
                movie_id INTEGER,
                actor_id INTEGER,
                order_index INTEGER DEFAULT 0,
                PRIMARY KEY (movie_id, actor_id),
                UNIQUE (movie_id, order_index)
            );
            """.trimIndent()
        )
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS user_movies (
                user_id VARCHAR(26),
                movie_id INTEGER,
                order_index INTEGER DEFAULT 0,
                PRIMARY KEY (user_id, movie_id),
                UNIQUE (user_id, order_index)
            );
            """.trimIndent()
        )
    }

    fun login(loginName :String, password :String): PreferencesDto {
        val users = jdbcTemplate.query(
            "SELECT id, name, login, avatar FROM users WHERE LOWER(login) = LOWER(?)",
            { rs, _ ->
                UserDto(
                    id= ULID.parseULID(rs.getString("id")),
                    name= rs.getString("name"),
                    login= rs.getString("login"),
                    avatar= rs.getString("avatar")?.let { Url(it) }
                )
            },
            loginName
        )

        val user = if (users.isNotEmpty()) {
            users[0]
        } else {
            val newUlid = ulidFactory.nextULID()
            jdbcTemplate.update(
                "INSERT INTO users (id, name, login, avatar) VALUES (?, ?, ?, ?)",
                newUlid.toString(),
                loginName,
                loginName,
                null
            )
            UserDto(
                id= newUlid,
                name= loginName,
                login= loginName
            )
        }

        val movies = movieService.getMoviesForUser(user.id)
        val resultMovies = movies.ifEmpty { knownMovies }

        return PreferencesDto(
            userId= user.id,
            movies= resultMovies.toMutableList()
        )
    }

    fun findUser(login :String) :UserDto? {
        val users = jdbcTemplate.query(
            "SELECT id, name, login, avatar FROM users WHERE login = ?",
            { rs, _ ->
                UserDto(
                    id= ULID.parseULID(rs.getString("id")),
                    name= rs.getString("name"),
                    login= rs.getString("login"),
                    avatar= rs.getString("avatar")?.let { Url(it) }
                )
            },
            login
        )
        return users.firstOrNull()
    }
}
