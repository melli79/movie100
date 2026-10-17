package de.gruetzmann.movie100

import de.gruetzmann.movie100.model.ActorDto
import de.gruetzmann.movie100.model.ErrorDto
import de.gruetzmann.movie100.model.Gender
import de.gruetzmann.movie100.model.Gender.FEMALE
import de.gruetzmann.movie100.model.Gender.MALE
import de.gruetzmann.movie100.model.Gender.OTHER
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import uk.co.conoregan.themoviedbapi.TmdbApi
import uk.co.conoregan.themoviedbapi.model.core.Movie
import de.gruetzmann.movie100.model.MovieDto
import de.gruetzmann.movie100.model.Url
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import uk.co.conoregan.themoviedbapi.model.movies.Cast
import uk.co.conoregan.themoviedbapi.model.movies.Crew
import kotlin.time.Instant

var apiKey :String? =null

fun main(args :Array<String>) {
    for (arg in args) when {
        arg.startsWith("--apiKey", true) -> {
            apiKey = arg.substringAfter("=")
        }
    }
    if (apiKey==null)
        println("TMDB API Key not set.")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

private fun queryMovie(tmbdClient :TmdbApi, titleQuery :String) :List<MovieDto> {
    val movies = tmbdClient.search.searchMulti(titleQuery, true, "en", 0)
    println("Found movies: " + movies.results)
    val results = mutableListOf<MovieDto>()
    for (item in movies.results) {
        val movie = item as? Movie ?: continue
        results.add(MovieDto(movie.id, movie.title,
            if (movie.posterPath.isNullOrBlank()) null  else Url("https://image.tmdb.org/t/p/w300"+movie.posterPath),
            description= if (!movie.overview.isNullOrBlank()) movie.overview  else null,
            releaseDate= if (!movie.releaseDate.isNullOrBlank()) Instant.parse(movie.releaseDate+"T00:00:00Z")  else null))
    }
    return results
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }
    var tmbdClient :TmdbApi? =null
    if (apiKey!=null) {
        tmbdClient = TmdbApi(apiKey)
    }

    routing {
        get("/search") {
            if (tmbdClient!=null) {
                val query = call.request.queryParameters["q"]
                if (query.isNullOrBlank())
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("Missing query parameter"))
                else {
                    val candidates = queryMovie(tmbdClient, query)
                    call.respond(HttpStatusCode.OK, candidates)
                }
            } else
                call.respond(HttpStatusCode.FailedDependency, ErrorDto("API key not set!"))
        }
        get("/credits/{id}") {
            if (tmbdClient!=null) {
                val idStr = call.parameters["id"]
                val id = idStr?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid or missing movie id"))
                } else {
                    val credits = tmbdClient.movies.getCredits(id, "en")
                    call.respond(HttpStatusCode.OK, MovieDto(id, "", null,
                        credits.cast.map(::toDto).toMutableList(), getDirector(credits.crew)))
                }
            } else
                call.respond(HttpStatusCode.FailedDependency, ErrorDto("API key not set!"))
        }
    }
}

fun toDto(cast :Cast) :ActorDto = ActorDto(cast.id, cast.name, toGender(cast.gender))

internal fun getDirector(crew :List<Crew>) :String? {
    for (member in crew) if (member.job.contains("director", true))
        return member.name
    return null
}

typealias TmdbGender = uk.co.conoregan.themoviedbapi.model.people.Gender
fun toGender(gender :TmdbGender) :Gender = when(gender) {
    TmdbGender.MALE -> MALE
    TmdbGender.FEMALE -> FEMALE
    else -> OTHER
}
