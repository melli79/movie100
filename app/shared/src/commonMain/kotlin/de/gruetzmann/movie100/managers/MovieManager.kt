package de.gruetzmann.movie100.managers

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import de.gruetzmann.movie100.model.ErrorDto
import de.gruetzmann.movie100.model.MovieDto
import de.gruetzmann.movie100.model.PreferencesDto
import de.gruetzmann.movie100.model.inits.knownMovies
import de.gruetzmann.movie100.model.years
import de.gruetzmann.movie100.viewmodel.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.decodeToImageBitmap
import ulid.ULID
import kotlin.random.Random
import kotlin.time.Clock

expect val serverUrl :String
val random = Random(Clock.System.now().epochSeconds)

object MovieManager {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val http = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    private val ulidFactory = ULID.Factory(random= random)
    private var movies = LruCache<Int, MovieDto>(100)
    private lateinit var login :String
    private lateinit var userId :ULID
    private var usersMovies = mutableListOf<MovieDto>()
    private var url2poster = LruCache<String, ImageBitmap>(300)
    private var loadAll = true
    private var subscribers = mutableListOf<(List<Movie>) -> Unit>()

    suspend fun login(login :String, password :String) :Boolean {
        if (login.isBlank() || password.isBlank())
            return false
        val preferences = http.post {
            url(serverUrl+"/login/$login")
        }.body<PreferencesDto?>() ?: PreferencesDto(ulidFactory.nextULID(), knownMovies.toMutableList())
        usersMovies.clear()
        preferences.movies.forEach { movie ->
            this.movies.put(movie.id, movie)
            addMovie(toMovie(movie), false)
        }
        this.login = login
        this.userId = preferences.userId
        notifySubscribers()
        return true
    }

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
        val movies = response.body<List<MovieDto>>()
        movies.forEach { movie ->
            this.movies.put(movie.id, movie)
        }
        return movies.map(this::toMovie)
    }

    fun addMovie(movie :Movie, notify :Boolean =true) :Boolean {
        if (usersMovies.none { it.id == movie.id }) {
            var candidate = movies.get(movie.id)
            if (candidate!=null) {
                usersMovies.add(candidate)
                if (notify) {
                    notifySubscribers()
                    savePreferences()
                }
            } else coroutineScope.launch {
                val candidates = http.get {
                    url(serverUrl+"/search?q=${movie.title}")
                }.body<List<MovieDto>>()
                candidate = candidates.firstOrNull { it.id==movie.id } ?: return@launch
                movies.put(candidate.id, candidate)
                usersMovies.add(candidate)
                if (notify) {
                    notifySubscribers()
                    savePreferences()
                }
            }

            if (loadAll && movie.posterUrl != null && url2poster.get(movie.posterUrl.location)==null) coroutineScope.launch {
                try { // TODO: prefer posters of visible movie descriptions
                    val bitmap = loadImageNow(movie.posterUrl.location)
                    if (bitmap != null) {
                        url2poster.put(movie.posterUrl.location, bitmap)
                        notifySubscribers()
                    }
                } catch (e: Throwable) {
                    println("Error loading poster image: ${e.message}")
                    if (e is OutOfMemoryError) {
                        url2poster.resize(100)
                        loadAll = false
                    }
                }
            }

            if (movie.actors.isEmpty()) coroutineScope.launch {
                try {
                    val crew = http.get("$serverUrl/credits/${movie.id}")
                    if (crew.status == HttpStatusCode.OK) {
                        val body = crew.body<MovieDto>()
                        val actors = body.actors.take(3)
                        val index = usersMovies.indexOfFirst { it.id == movie.id }
                        if (index >= 0) {
                            usersMovies[index] = usersMovies[index].copy(actors= actors.toMutableList(), director= body.director)
                            notifySubscribers()
                        }
                    } else {
                        println("Credits error ${crew.status}: ${crew.body<ErrorDto>().message}")
                    }
                } catch (e: Throwable) {
                    println("Error fetching credits for movie ${movie.id}: ${e.message}")
                }
            }
            return true
        }
        return false
    }

    fun savePreferences() = coroutineScope.launch {
        http.put("$serverUrl/preferences/$userId") {
            contentType(ContentType.Application.Json)
            setBody(PreferencesDto(userId, usersMovies.toMutableList()))
        }
    }

    fun subscribeUsersMovies(login :String, subscriber :(List<Movie>) -> Unit) {
        subscribers.add(subscriber)
        if (usersMovies.isNotEmpty())
            subscriber(usersMovies.map(this::toMovie))
    }

    private fun loadKnownMovies() {
        knownMovies.forEach { movie ->
            movies.put(movie.id, movie)
            usersMovies.add(movie)
        }
        notifySubscribers()
        coroutineScope.launch {
            usersMovies.forEachIndexed { index, movie ->
                val posterUrl = movie.poster
                if (posterUrl!=null) launch {
                    val bitmap = loadImageNow(posterUrl.location)
                    if (bitmap != null) {
                        url2poster.put(posterUrl.location, bitmap)
                        notifySubscribers()
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
        poster= if (dto.poster!=null) url2poster.get(dto.poster!!.location)  else null,
        details = dto.description
    )

    fun loadImage(movie :Movie) {
        if (movie.posterUrl != null && url2poster.get(movie.posterUrl.location)==null) coroutineScope.launch {
            try {
                val bitmap = loadImageNow(movie.posterUrl.location)
                if (bitmap != null) {
                    url2poster.put(movie.posterUrl.location, bitmap)
                    notifySubscribers()
                }
            } catch (e :Throwable) {
                e.printStackTrace()
                println("Error loading poster image: ${e.message}")
            }
        }
    }

    suspend fun loadImageNow(poster :String) :ImageBitmap? {
        return try {
            val jpg :ByteArray = http.get(poster).body()
            jpg.decodeToImageBitmap()
        } catch (e :Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun reorderMovies(fromIndex :Int, toIndex :Int) {
        if (fromIndex in usersMovies.indices && toIndex in usersMovies.indices && fromIndex != toIndex) {
            val item = usersMovies.removeAt(fromIndex)
            usersMovies.add(toIndex, item)
            coroutineScope.launch {
                http.put("$serverUrl/preferences/$login") {
                    contentType(ContentType.Application.Json)
                    setBody(PreferencesDto(userId, usersMovies.toMutableList()))
                }
            }
            notifySubscribers()
            savePreferences()
        }
    }

    private fun notifySubscribers() {
        mainScope.launch {
            val currentList = usersMovies.map(::toMovie)
            for (subscriber in subscribers)
                subscriber(currentList)
        }
    }

    fun moveMovieUp(index :Int) {
        if (index > 0 && index < usersMovies.size) {
            reorderMovies(index, index - 1)
        }
    }

    fun moveMovieDown(index :Int) {
        if (index >= 0 && index < usersMovies.size - 1) {
            reorderMovies(index, index + 1)
        }
    }

    fun removeUserMovie(movie :Movie) {
        val candidate = usersMovies.firstOrNull { it.id == movie.id } ?: return
        usersMovies.remove(candidate)
        coroutineScope.launch {
            http.put {
                url("$serverUrl/preferences/$login")
                contentType(ContentType.Application.Json)
                setBody(PreferencesDto(userId, usersMovies.toMutableList()))
            }
        }
        notifySubscribers()
    }

    suspend fun deleteUser() :Boolean {
        http.delete("$serverUrl/user/$login") {
            contentType(ContentType.Text.Plain)
            setBody("DELETE")
        }
        return true
    }
}
