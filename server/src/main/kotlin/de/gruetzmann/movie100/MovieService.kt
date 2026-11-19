package de.gruetzmann.movie100

import com.google.common.cache.CacheBuilder
import de.gruetzmann.movie100.db.ActorsTable
import de.gruetzmann.movie100.db.MovieActorsTable
import de.gruetzmann.movie100.db.MoviesTable
import de.gruetzmann.movie100.model.ActorDto
import de.gruetzmann.movie100.model.Gender
import de.gruetzmann.movie100.model.Gender.FEMALE
import de.gruetzmann.movie100.model.Gender.MALE
import de.gruetzmann.movie100.model.Gender.OTHER
import de.gruetzmann.movie100.model.MovieDto
import de.gruetzmann.movie100.model.Url
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import uk.co.conoregan.themoviedbapi.TmdbApi
import uk.co.conoregan.themoviedbapi.model.core.Movie
import uk.co.conoregan.themoviedbapi.model.movies.Cast
import uk.co.conoregan.themoviedbapi.model.movies.Crew
import uk.co.conoregan.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toJavaDuration

class MovieService(private val tmdbClient :TmdbApi) {
    private val movieCache = CacheBuilder.newBuilder()
        .maximumSize(100)  // max entries to keep
        .expireAfterWrite((5.minutes).toJavaDuration())
        .build<Int, MovieDto>()

    fun queryMovie(titleQuery :String) :List<MovieDto> {
        val movies = tmdbClient.search.searchMulti(titleQuery, true, "en", 0)
        val results = mutableListOf<MovieDto>()
        for (item in movies.results) {
            val movie = item as? Movie ?: continue
            results.add(toDto(movie))
        }
        return results
    }

    fun getFullMovie(id :Int) :MovieDto {
        var candidate = movieCache.getIfPresent(id)
        if (candidate == null) {
            candidate = transaction {
                val movieRow = MoviesTable.selectAll()
                    .firstOrNull { it[MoviesTable.id] == id }
                val actorsPo = if (movieRow != null) (MovieActorsTable innerJoin ActorsTable)
                    .selectAll()
                    .where { MovieActorsTable.movieId eq id }
                    .orderBy(MovieActorsTable.orderIndex to SortOrder.ASC)
                else null

                if (movieRow != null) {
                    var director :String? = null
                    val actors :List<ActorDto> = actorsPo?.map {
                        ActorDto(
                            id = it[ActorsTable.id], name = it[ActorsTable.name],
                            gender = it[ActorsTable.gender], birthyear = it[ActorsTable.birthyear]?.toInt(),
                            specialty = it[ActorsTable.specialty]
                        )
                    } ?: tmdbClient.movies.getCredits(id, "en").let { credits ->
                        director = getDirector(credits.crew)
                        credits.cast.take(3).map(::toDto)
                    }
                    return@transaction MovieDto(
                        id = movieRow[MoviesTable.id],
                        title = movieRow[MoviesTable.title],
                        poster = movieRow[MoviesTable.poster]?.let { Url(it) },
                        actors = actors.toMutableList(),
                        director = movieRow[MoviesTable.director] ?: director,
                        releaseDate = movieRow[MoviesTable.releaseDate]?.let { Instant.fromEpochSeconds(it.toLong()) },
                        description = movieRow[MoviesTable.description]
                    )
                }
                val qr = tmdbClient.movies.getDetails(id, "en", MovieAppendToResponse.CREDITS)
                val result = MovieDto(
                    qr.id, qr.title, qr.posterPath?.let { Url("https://image.tmdb.org/t/p/w300$it") },
                    qr.credits.cast.take(3).map(::toDto).toMutableList(),
                    getDirector(qr.credits.crew), Instant.parse(qr.releaseDate + "T0:00:00"),
                    description = if (qr.overview.isNullOrBlank()) null else qr.overview
                )
                movieCache.put(qr.id, result)
                MoviesTable.upsert {
                    it[MoviesTable.id] = result.id
                    it[title] = result.title
                    it[poster] = result.poster?.location
                    it[director] = result.director
                    it[MoviesTable.releaseDate] = result.releaseDate?.epochSeconds?.toULong()
                    it[description] = result.description
                }
                result.actors.forEachIndexed { index, dto ->
                    ActorsTable.upsert {
                        it[ActorsTable.id] = dto.id
                        it[name] = dto.name
                        it[gender] = dto.gender
                        it[birthyear] = dto.birthyear?.toUInt()
                    }
                    MovieActorsTable.upsert {
                        it[MovieActorsTable.movieId] = result.id
                        it[MovieActorsTable.orderIndex] = index
                        it[MovieActorsTable.actorId] = dto.id
                    }
                }
                return@transaction result
            }
        }
        if (candidate.actors.isEmpty()) transaction {
            val actorsRows = (MovieActorsTable innerJoin ActorsTable)
                .selectAll()
                .where { MovieActorsTable.movieId eq id }
                .orderBy(MovieActorsTable.orderIndex to SortOrder.ASC)
            if (!actorsRows.empty())
                candidate.actors.addAll(actorsRows.map {
                    ActorDto(
                        id = it[ActorsTable.id], name = it[ActorsTable.name], gender = it[ActorsTable.gender],
                        birthyear = it[ActorsTable.birthyear]?.toInt(), specialty = it[ActorsTable.specialty]
                    )
                })
            else {
                val qr = tmdbClient.movies.getCredits(id, "en")
                candidate.actors.addAll(qr.cast.take(3).map(::toDto))
                candidate.director = getDirector(qr.crew)
                if (candidate.director != null) MoviesTable.upsert {
                    it[MoviesTable.id] = candidate.id
                    it[title] = candidate.title
                    it[poster] = candidate.poster?.location
                    it[director] = candidate.director
                    it[releaseDate] = candidate.releaseDate?.epochSeconds?.toULong()
                    it[description] = candidate.description
                }
                candidate.actors.forEachIndexed { index, actor ->
                    ActorsTable.upsert {
                        it[ActorsTable.id] = actor.id
                        it[ActorsTable.name] = actor.name
                        it[ActorsTable.gender] = actor.gender
                        it[ActorsTable.birthyear] = actor.birthyear?.toUInt()
                        it[ActorsTable.specialty] = actor.specialty
                    }
                    MovieActorsTable.upsert {
                        it[MovieActorsTable.movieId] = id
                        it[MovieActorsTable.orderIndex] = index
                        it[MovieActorsTable.actorId] = actor.id
                    }
                }
            }
            return@transaction candidate
        }
        return candidate
    }

    private fun toDto(movie :Movie) = MovieDto(
        movie.id, movie.title,
        if (movie.posterPath.isNullOrBlank()) null else Url("https://image.tmdb.org/t/p/w300" + movie.posterPath),
        description = if (!movie.overview.isNullOrBlank()) movie.overview else null,
        releaseDate = if (!movie.releaseDate.isNullOrBlank()) Instant.parse(movie.releaseDate + "T00:00:00Z") else null
    )

    internal fun toDto(cast :Cast) :ActorDto = ActorDto(cast.id, cast.name, toGender(cast.gender))

    typealias TmdbGender = uk.co.conoregan.themoviedbapi.model.people.Gender

    internal fun toGender(gender :TmdbGender) :Gender = when (gender) {
        TmdbGender.MALE -> MALE
        TmdbGender.FEMALE -> FEMALE
        else -> OTHER
    }

    internal fun getDirector(crew :List<Crew>) :String? {
        for (member in crew) if (member.department.contains("directing", true) &&
            member.job.contains("director", true) && !member.job.contains("assistant", true)
        )
            return member.name
        return null
    }

    companion object {
        fun exportAll(directory :String) :Unit = transaction {
            val dir = File(directory)
            val moviesFile = File(dir, "movies.csv")
            val mout = PrintStream(FileOutputStream(moviesFile))
            mout.println("id;title;director;release_date")
            MoviesTable.selectAll().sortedBy { it[MoviesTable.id] }.forEach {
                mout.print(it[MoviesTable.id]); mout.print(';')
                mout.print("\"" + it[MoviesTable.title] + "\";")
                mout.print(it[MoviesTable.director]?.let { "\"$it\";" } ?: ";")
                mout.print(it[MoviesTable.releaseDate] ?: 0uL)
                mout.println()
            }
            mout.close()
            val aout = PrintStream(FileOutputStream(File(dir, "actors.csv")))
            aout.println("id;name;gender;birthyear")
            ActorsTable.selectAll().sortedBy { it[ActorsTable.id] }.forEach {
                aout.print(it[ActorsTable.id]); aout.print(';')
                aout.print("\"" + it[ActorsTable.name] + "\";")
                aout.print(it[ActorsTable.gender].toString() + ";")
                aout.print(it[ActorsTable.birthyear] ?: ""); aout.print(';')
                aout.println()
            }
            aout.close()
            val m2a = PrintStream(FileOutputStream(File(dir, "m2a.csv")))
            m2a.println("movie_id;actor_id")
            MovieActorsTable.selectAll().sortedBy { it[MovieActorsTable.movieId] }.forEach {
                m2a.println("${it[MovieActorsTable.movieId]};${it[MovieActorsTable.actorId]}")
            }
        }
    }
}