package org.grutzmann.movie100.springserver

import de.gruetzmann.movie100.model.ActorDto
import de.gruetzmann.movie100.model.Gender
import de.gruetzmann.movie100.model.MovieDto
import de.gruetzmann.movie100.model.Url
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import uk.co.conoregan.themoviedbapi.TmdbApi
import uk.co.conoregan.themoviedbapi.model.core.Movie
import ulid.ULID
import kotlin.time.Instant

@Service
class MovieService(
    private val tmdbClient :TmdbApi,
    private val jdbcTemplate :JdbcTemplate
) {
    fun searchMovie(titleQuery :String) :List<MovieDto> {
        val movies = tmdbClient.search.searchMulti(titleQuery, true, "en", 0)
        val results = mutableListOf<MovieDto>()
        for (item in movies.results) {
            val movie = item as? Movie ?: continue
            results.add(toDto(movie))
        }
        return results

    }

    private fun toDto(tmdb :Movie) = MovieDto(
        tmdb.id, tmdb.title,
        if (tmdb.posterPath.isNullOrBlank()) null else Url("https://image.tmdb.org/t/p/w300" + tmdb.posterPath),
        description = if (!tmdb.overview.isNullOrBlank()) tmdb.overview else null,
        releaseDate = if (!tmdb.releaseDate.isNullOrBlank()) Instant.parse(tmdb.releaseDate + "T00:00:00Z") else null
    )

    fun getMoviesForUser(userId :ULID) :List<MovieDto> {
        val movies = jdbcTemplate.query(
            """
        SELECT m.id, m.title, m.poster, m.director, m.release_date, m.description 
        FROM user_movies um 
        JOIN movies m ON um.movie_id = m.id 
        WHERE um.user_id = ? 
        ORDER BY um.order_index ASC
        """.trimIndent(),
            { rs, _ ->
                val movieId = rs.getInt("id")
                val releaseDateLong = rs.getLong("release_date")
                val releaseDate = if (rs.wasNull()) null else Instant.fromEpochSeconds(releaseDateLong)

                MovieDto(
                    id = movieId,
                    title = rs.getString("title"),
                    poster = rs.getString("poster")?.let { Url(it) },
                    actors = mutableListOf(),
                    director = rs.getString("director"),
                    releaseDate = releaseDate,
                    description = rs.getString("description")
                )
            },
            userId.toString()
        )

        for (movie in movies) {
            val actors = getActorsForMovie(movie.id)
            movie.actors.addAll(actors)
        }

        return movies.toMutableList()
    }

    fun getActorsForMovie(movieId :Int) :List<ActorDto> = jdbcTemplate.query(
        """
            SELECT a.id, a.name, a.gender, a.birthyear, a.specialty 
            FROM movie_actors ma 
            JOIN actors a ON ma.actor_id = a.id 
            WHERE ma.movie_id = ? 
            ORDER BY ma.order_index ASC
            """.trimIndent(),
        { rs, _ ->
            val birthyearVal = rs.getInt("birthyear")
            val birthyear = if (rs.wasNull()) null else birthyearVal
            val genderStr = rs.getString("gender")
            val gender = try {
                Gender.valueOf(genderStr)
            } catch (e :Exception) {
                Gender.MALE
            }

            ActorDto(
                id= rs.getInt("id"),
                name= rs.getString("name"),
                gender= gender,
                birthyear= birthyear,
                specialty= rs.getString("specialty")
            )
        },
        movieId.toString()
    )
}
