package de.gruetzmann.movie100

import de.gruetzmann.movie100.db.DatabaseFactory
import de.gruetzmann.movie100.model.ActorDto
import de.gruetzmann.movie100.model.ErrorDto
import de.gruetzmann.movie100.model.PreferencesDto
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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import kotlinx.serialization.json.Json
import uk.co.conoregan.themoviedbapi.model.movies.Cast
import uk.co.conoregan.themoviedbapi.model.movies.Crew
import ulid.ULID
import kotlin.random.Random
import kotlin.time.Instant

val random = Random(System.currentTimeMillis())
val ulidFactory = ULID.Factory(random)
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

private fun queryMovie(tmdbClient :TmdbApi, titleQuery :String) :List<MovieDto> {
    val movies = tmdbClient.search.searchMulti(titleQuery, true, "en", 0)
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
    DatabaseFactory.init()
    install(CORS) {
        allowHost("tmdb.org", subDomains= listOf("image", "www"))
        allowHost("localhost:8080")
        allowHost("localhost:8081")
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }
    var tmdbClient :TmdbApi? =null
    if (apiKey!=null) {
        tmdbClient = TmdbApi(apiKey)
    }
    val userService = UserService

    routing {
        get("/search") {
            if (tmdbClient!=null) {
                val query = call.request.queryParameters["q"]
                if (query.isNullOrBlank())
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("Missing query parameter"))
                else {
                    val candidates = queryMovie(tmdbClient, query)
                    call.respond(HttpStatusCode.OK, candidates)
                }
            } else
                call.respond(HttpStatusCode.FailedDependency, ErrorDto("API key not set!"))
        }
        get("/credits/{id}") {
            if (tmdbClient!=null) {
                val idStr = call.parameters["id"]
                val id = idStr?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid or missing movie id"))
                } else {
                    val credits = tmdbClient.movies.getCredits(id, "en")
                    call.respond(HttpStatusCode.OK, MovieDto(id, "", null,
                        credits.cast.map(::toDto).toMutableList(), getDirector(credits.crew)))
                }
            } else
                call.respond(HttpStatusCode.FailedDependency, ErrorDto("API key not set!"))
        }
        post("/login/{user}") {
            val login = call.request.pathVariables["user"] ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorDto("Missing login"))
            val user = userService.login(login)
            val preferences = UserService.getPreferences(user)
            call.respond(HttpStatusCode.Accepted, preferences)
        }

        put("/preferences/{userId}") {
            try {
                val preferences = call.receive<PreferencesDto>()
                val saved = userService.savePreferences(preferences)
                call.respond(HttpStatusCode.OK, saved)
            } catch (e :Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid or missing JSON payload: ${e.message}"))
            }
        }

        delete("/user/{user}") {
            val login = call.request.pathVariables["user"] ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorDto("Missing login"))
            val confirm = call.receive<String>()
            if (confirm!="DELETE")
                return@delete call.respond(HttpStatusCode.BadRequest, ErrorDto("missing confirmation"))
            if (userService.deleteUser(login))
                return@delete call.respond(HttpStatusCode.Accepted, "User '$login' deleted.")
            else
                return@delete call.respond(HttpStatusCode.NotFound, ErrorDto("User '$login' not found."))
        }
    }
}

fun toDto(cast :Cast) :ActorDto = ActorDto(cast.id, cast.name, toGender(cast.gender))

internal fun getDirector(crew :List<Crew>) :String? {
    for (member in crew) if (member.department.contains("directing", true) && member.job.contains("director", true) && !member.job.contains("assistant", true))
        return member.name
    return null
}

typealias TmdbGender = uk.co.conoregan.themoviedbapi.model.people.Gender
fun toGender(gender :TmdbGender) :Gender = when(gender) {
    TmdbGender.MALE -> MALE
    TmdbGender.FEMALE -> FEMALE
    else -> OTHER
}
