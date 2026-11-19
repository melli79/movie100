package org.grutzmann.movie100.springserver

import de.gruetzmann.movie100.model.ActorDto
import de.gruetzmann.movie100.model.Gender
import de.gruetzmann.movie100.model.Gender.FEMALE
import de.gruetzmann.movie100.model.Gender.MALE
import de.gruetzmann.movie100.model.Gender.OTHER
import de.gruetzmann.movie100.model.MovieDto
import de.gruetzmann.movie100.model.Url
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import uk.co.conoregan.themoviedbapi.TmdbApi
import uk.co.conoregan.themoviedbapi.model.core.Movie
import uk.co.conoregan.themoviedbapi.model.movies.Cast
import uk.co.conoregan.themoviedbapi.model.movies.Crew
import uk.co.conoregan.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse
import ulid.ULID
import kotlin.time.Instant

@Service
class MovieService(
    private val tmdbClient :TmdbApi,
    private val jdbcTemplate :JdbcTemplate,
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
        description= if (!tmdb.overview.isNullOrBlank()) tmdb.overview else null,
        releaseDate= if (!tmdb.releaseDate.isNullOrBlank()) Instant.parse(tmdb.releaseDate + "T00:00:00Z") else null
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
                    id= movieId,
                    title= rs.getString("title"),
                    poster= rs.getString("poster")?.let { Url(it) },
                    actors= mutableListOf(),
                    director= rs.getString("director"),
                    releaseDate= releaseDate,
                    description= rs.getString("description")
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

    @Cacheable(cacheNames= ["movies"], key= "#id")
    fun getFullMovie(id :Int) :MovieDto {
        val qr = tmdbClient.movies.getDetails(id, "en", MovieAppendToResponse.CREDITS)
        val result = MovieDto(
            qr.id, qr.title, qr.posterPath?.let { Url("https://image.tmdb.org/t/p/w300$it") },
            qr.credits.cast.take(3).map(::toDto).toMutableList(),
            getDirector(qr.credits.crew), Instant.parse(qr.releaseDate + "T00:00:00Z"),
            description = if (qr.overview.isNullOrBlank()) null else qr.overview
        )
        val actors = qr.credits.cast.map(::toDto)
        result.actors.addAll(actors)
        return result
    }

    internal fun getDirector(crew :List<Crew>) :String? {
        for (member in crew) if (member.department.contains("directing", true) &&
            member.job.contains("director", true) && !member.job.contains("assistant", true)
        )
            return member.name
        return null
    }

    @Cacheable(cacheNames= ["actors"], key="#id")
    fun getActor(id :Int) :ActorDto {
        val qr = tmdbClient.people.getDetails(id, "en")
        return ActorDto(qr.id, name= qr.name, gender= toGender(qr.gender),
            birthyear= qr.birthday?.substring(0..<4)?.toIntOrNull(),
            specialty= qr.biography.substring(0..<512))
    }

    @CachePut(cacheNames= ["actors"], key= "#cast.id")
    internal fun toDto(cast :Cast) :ActorDto = ActorDto(cast.id, cast.name, toGender(cast.gender))

    typealias TmdbGender = uk.co.conoregan.themoviedbapi.model.people.Gender

    internal fun toGender(gender :TmdbGender) :Gender = when (gender) {
        TmdbGender.MALE -> MALE
        TmdbGender.FEMALE -> FEMALE
        else -> OTHER
    }
}
