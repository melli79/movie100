package de.gruetzmann.movie100

import de.gruetzmann.movie100.db.DatabaseFactory
import de.gruetzmann.movie100.db.UsersTable
import de.gruetzmann.movie100.model.ErrorDto
import de.gruetzmann.movie100.model.PreferencesDto
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import uk.co.conoregan.themoviedbapi.TmdbApi
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import ulid.ULID
import kotlin.random.Random
import kotlin.system.exitProcess

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
    val movieService = if (tmdbClient!=null) MovieService(tmdbClient)  else null
//    MovieService.exportAll(".")
//    exitProcess(0)

    routing {
        get("/search") {
            if (movieService!=null) {
                val query = call.request.queryParameters["q"]
                if (query.isNullOrBlank())
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("Missing query parameter"))
                else {
                    val candidates = movieService.queryMovie(query)
                    call.respond(HttpStatusCode.OK, candidates)
                }
            } else
                call.respond(HttpStatusCode.FailedDependency, ErrorDto("API key not set!"))
        }
        get("/credits/{id}") {
            if (movieService!=null) {
                val idStr = call.parameters["id"]
                val id = idStr?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid or missing movie id"))
                } else {
                    val movie = movieService.getFullMovie(id)
                    call.respond(HttpStatusCode.OK, movie)
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

        put("/preferences/{login}") {
            if (movieService!=null) try {
                val login = call.pathParameters.get("login")
                if (login.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("missing login path attribute"))
                    return@put
                }
                val user = userService.findUser(login)
                if (user==null) {
                    call.respond(HttpStatusCode.NotFound, ErrorDto("User not found. Did you login?"))
                    return@put
                }
                val userId = user.id
                println("User id: $userId")
                val preferences = call.receive<PreferencesDto>()
                if (preferences.userId!=userId) {
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("User does not match its login"))
                    return@put
                }
                val saved = movieService.savePreferences(preferences)
                call.respond(HttpStatusCode.OK, saved)
            } catch (e :Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid or missing JSON payload: ${e.message}"))
            } else
                call.respond(HttpStatusCode.FailedDependency, ErrorDto("API key not set!"))
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

        singlePageApplication {
            useResources = true
            filesPath = "static"
            defaultPage = "index.html"
        }
    }
}
