package de.gruetzmann.movie100.managers

import androidx.compose.ui.graphics.ImageBitmap
import de.gruetzmann.movie100.model.ActorDto
import de.gruetzmann.movie100.model.ErrorDto
import de.gruetzmann.movie100.model.Gender
import de.gruetzmann.movie100.model.MovieDto
import de.gruetzmann.movie100.model.Url
import de.gruetzmann.movie100.model.years
import de.gruetzmann.movie100.viewmodel.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.time.Instant

expect val serverUrl :String

object MovieManager {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val http = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
    private var knownMovies = mutableListOf(
        MovieDto(1, "Star Trek IX First Contact",
        Url("https://image.tmdb.org/t/p/w300/iqhHe893Vcf07jNkNQ31tu85dKG.jpg"),
        mutableListOf(ActorDto(1,"Patrick Steward", Gender.MALE),
            ActorDto(2,"Brent Spiner", Gender.MALE),
            ActorDto(3,"Jonathan Frakes", Gender.MALE),
            ActorDto(4,"LeVar Burton", Gender.MALE)),
        "J. Frakes", Instant.parse("1996-11-02T00:00:00Z")),
        MovieDto(2, "The Lord of the Rings I The Fellowship",
        Url("https://image.tmdb.org/t/p/w300/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg"),
        mutableListOf(ActorDto(5,"Elijah Wood", Gender.MALE),
            ActorDto(6,"Ian McKellen", Gender.MALE),
            ActorDto(7,"Orlando Bloom", Gender.MALE)),
            "", Instant.parse("2001-12-01T00:00:00Z")),
        MovieDto(3, "The Godfather",
        Url("https://image.tmdb.org/t/p/w300/3bhkrj58Vtu7enYsRolD1fZdja1.jpg"),
        mutableListOf(ActorDto(8,"Marlon Brando", Gender.MALE),
            ActorDto(9,"Al Pacino", Gender.MALE)),
            "F.F. Coppola", Instant.parse("1972-10-01T00:00:00Z")),
        MovieDto(4, "The Green Butcher",
        Url("https://image.tmdb.org/t/p/w300/w9hCJ0JKftF8nX9Z2gNbVOtCFh1.jpg"),
        mutableListOf(ActorDto(10,"Christian Bale", Gender.MALE),
            ActorDto(11,"Nikolaj Lie Kaas", Gender.MALE),
            ActorDto(12,"Mads Mikkelsen", Gender.MALE),
            ActorDto(13,"Line Kruse", Gender.FEMALE)),
                    "A.Th. Jenssen", Instant.parse("2003-10-01T00:00:00Z")),
        MovieDto(5, "The Dark Knight",
        Url("https://image.tmdb.org/t/p/w300/qJ2tW6WMUDux911r6m7haRef0WH.jpg"),
        mutableListOf(ActorDto(14,"Christian Bale", Gender.MALE),
            ActorDto(15,"Heath Ledger", Gender.MALE),
            ActorDto(16,"Aaron Eckhart", Gender.MALE)),
            "Christopher Nolan", Instant.parse("2008-10-01T00:00:00Z")),
    )
    private var movies = mutableListOf<Movie>()
    private var usersMovies = mutableListOf<Movie>()
    private var subscribers = mutableListOf<(List<Movie>) -> Unit>()

    suspend fun findByNameLike(pattern :String) :List<Movie> {
        val response = http.get("$serverUrl/search") {
            url {
                parameters.append("q", pattern)
            }
            contentType(ContentType.Application.Json)
        }
        if (response.status!=HttpStatusCode.OK) {
            println("Server error ${response.status}: ${response.body<ErrorDto>().message}")
            return emptyList()
        }
        return response.body<List<MovieDto>>().map(this::toMovie)
    }

    fun addMovie(movie :Movie) :Boolean {
        if (movie !in usersMovies) {
            usersMovies.add(movie)
            notifySubscribers()
            if (movie.poster==null) coroutineScope.launch {
                val index = usersMovies.indexOf(movie)
                if (movie.poster==null && index>=0) {
                    val posterUrl = movie.posterUrl
                    if (posterUrl != null) launch {
                        val bitmap = loadImage(posterUrl.location)
                        if (bitmap != null) {
                            val updated = usersMovies[index].copy(poster = bitmap)
                            usersMovies[index] = updated
                            notifySubscribers()
                        }
                    }
                }
                if (movie.actors.isEmpty()) coroutineScope.launch {
                    val crew = http.get("$serverUrl/credits/${movie.id}")
                    if (crew.status== HttpStatusCode.OK) {
                        val body = crew.body<MovieDto>()
                        val actors = body.actors.take(3).map { it.toString() }
                        val index = usersMovies.indexOf(movie)
                        if (index != -1) {
                            usersMovies[index] = usersMovies[index].copy(actors= actors, director= body.director)
                            notifySubscribers()
                        }
                    }
                }
            }
            return true
        }
        return false
    }

    fun subscribeUsersMovies(login :String, subscriber :(List<Movie>) -> Unit) {
        subscribers.add(subscriber)
        if (usersMovies.isEmpty())
             loadKnownMovies()
        else
            subscriber(usersMovies.toList())
    }

    private fun loadKnownMovies() {
        movies = knownMovies.map(this::toMovie).toMutableList()
        usersMovies = movies.take(3).toMutableList()
        notifySubscribers()
        coroutineScope.launch {
            movies.forEachIndexed { index, movie ->
                val posterUrl = movie.posterUrl
                if (posterUrl!=null) launch {
                    val bitmap = loadImage(posterUrl.location)
                    if (bitmap != null) {
                        val updated = movie.copy(poster = bitmap)
                        movies[index] = updated
                        val userIdx = usersMovies.indexOfFirst { it.id == movie.id }
                        if (userIdx != -1) {
                            usersMovies[userIdx] = updated
                            notifySubscribers()
                        }
                    }
                }
            }
        }
    }

    private fun toMovie(dto :MovieDto) = Movie(
        id = dto.id,
        title = dto.title,
        actors = dto.actors.map { it.toString() },
        inceptionYear = dto.releaseDate?.years(),
        director = dto.director,
        posterUrl = dto.poster,
        details = dto.description
    )

    suspend fun loadImage(poster :String) :ImageBitmap? {
        return try {
            val jpg :ByteArray = http.get(poster).body()
            jpg.decodeToImageBitmap()
        } catch (e :Exception) {
            e.printStackTrace()
            null
        }
    }

    fun reorderMovies(login :String, fromIndex :Int, toIndex :Int) {
        if (fromIndex in usersMovies.indices && toIndex in usersMovies.indices && fromIndex != toIndex) {
            val item = usersMovies.removeAt(fromIndex)
            usersMovies.add(toIndex, item)
            notifySubscribers()
        }
    }

    private fun notifySubscribers() {
        mainScope.launch {
            val currentList = usersMovies.toList()
            for (subscriber in subscribers)
                subscriber(currentList)
        }
    }

    fun moveMovieUp(login :String, index :Int) {
        if (index > 0 && index < usersMovies.size) {
            reorderMovies(login, index, index - 1)
        }
    }

    fun moveMovieDown(login :String, index :Int) {
        if (index >= 0 && index < usersMovies.size - 1) {
            reorderMovies(login, index, index + 1)
        }
    }

    fun removeUserMovie(login :String, movie :Movie) {
        if (usersMovies.remove(movie))
            notifySubscribers()
    }
}
