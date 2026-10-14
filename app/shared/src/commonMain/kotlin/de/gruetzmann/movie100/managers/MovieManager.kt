package de.gruetzmann.movie100.managers

import androidx.compose.ui.graphics.ImageBitmap
import de.gruetzmann.movie100.model.ActorDto
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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import org.jetbrains.compose.resources.decodeToImageBitmap
import ulid.ULID
import kotlin.random.Random
import kotlin.time.Clock.System.now
import kotlin.time.Instant

object MovieManager {
    private val ulidFactory = ULID.Factory(random= Random(now().toEpochMilliseconds()))
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val http = HttpClient(CIO)
    private var knownMovies = mutableListOf(
        MovieDto(ulidFactory.nextULID(), "Star Trek IX First Contact",
            Url("https://image.tmdb.org/t/p/w1280/iqhHe893Vcf07jNkNQ31tu85dKG.jpg"),
            mutableListOf(ActorDto(ulidFactory.nextULID(),"Patrick Steward", Gender.MALE),
                ActorDto(ulidFactory.nextULID(),"Brent Spiner", Gender.MALE),
                ActorDto(ulidFactory.nextULID(),"Jonathan Frakes", Gender.MALE),
                ActorDto(ulidFactory.nextULID(),"LeVar Burton", Gender.MALE)),
            "J. Frakes", Instant.parse("1996-11-02T00:00:00Z")),
        MovieDto(ulidFactory.nextULID(), "The Lord of the Rings I The Fellowship",
        Url("https://image.tmdb.org/t/p/w1280/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg"),
        mutableListOf(ActorDto(ulidFactory.nextULID(),"Elijah Wood", Gender.MALE),
            ActorDto(ulidFactory.nextULID(),"Ian McKellen", Gender.MALE),
            ActorDto(ulidFactory.nextULID(),"Orlando Bloom", Gender.MALE)),
            "", Instant.parse("2001-12-01T00:00:00Z")),
        MovieDto(ulidFactory.nextULID(), "The Godfather",
        Url("https://image.tmdb.org/t/p/w1280/3bhkrj58Vtu7enYsRolD1fZdja1.jpg"),
        mutableListOf(ActorDto(ulidFactory.nextULID(),"Marlon Brando", Gender.MALE),
            ActorDto(ulidFactory.nextULID(),"Al Pacino", Gender.MALE)),
            "F.F. Coppola", Instant.parse("1972-10-01T00:00:00Z")),
        MovieDto(ulidFactory.nextULID(), "The Green Butcher",
        Url("https://image.tmdb.org/t/p/w1280/w9hCJ0JKftF8nX9Z2gNbVOtCFh1.jpg"),
        mutableListOf(ActorDto(ulidFactory.nextULID(),"Christian Bale", Gender.MALE),
            ActorDto(ulidFactory.nextULID(),"Nikolaj Lie Kaas", Gender.MALE),
            ActorDto(ulidFactory.nextULID(),"Mads Mikkelsen", Gender.MALE),
            ActorDto(ulidFactory.nextULID(),"Line Kruse", Gender.FEMALE)),
                    "A.Th. Jenssen", Instant.parse("2003-10-01T00:00:00Z")),
        MovieDto(ulidFactory.nextULID(), "The Dark Knight", Url(""),
        mutableListOf(ActorDto(ulidFactory.nextULID(),"Christian Bale", Gender.MALE),
            ActorDto(ulidFactory.nextULID(),"Heath Ledger", Gender.MALE),
            ActorDto(ulidFactory.nextULID(),"Aaron Eckhart", Gender.MALE)),
            "Christopher Nolan", Instant.parse("2008-10-01T00:00:00Z")),
    )
    private var movies = mutableListOf<Movie>()
    private var usersMovies = mutableListOf<Movie>()
    private var subscribers = mutableListOf<(List<Movie>) -> Unit>()

    fun findByNameLike(pattern :String) :List<Movie> = movies.filter { it.name.contains(pattern) }

    fun addMovie(login :String, movie :Movie) :Boolean {
        if (movie !in usersMovies) {
            usersMovies.add(movie)
            notifySubscribers()
            return true
        }
        return false
    }

    fun subscribeUsersMovies(login :String, subscriber :(List<Movie>) -> Unit) {
        subscribers.add(subscriber)
        if (movies.isEmpty())
             loadKnownMovies()
        else
            subscriber(usersMovies.toList())
    }

    private fun loadKnownMovies() {
        movies = knownMovies.map { dto ->
            Movie(
                id= dto.id,
                name= dto.name,
                actors= dto.actors.map { it.toString() },
                inceptionYear= dto.releaseDate.years(),
                director= dto.director,
                posterUrl= dto.poster?.location,
                details= dto.description
            )
        }.toMutableList()
        usersMovies = movies.take(3).toMutableList()
        notifySubscribers()
        coroutineScope.launch {
            movies.forEachIndexed { index, movie ->
                val posterUrl = movie.posterUrl
                if (!posterUrl.isNullOrEmpty()) launch {
                    val bitmap = loadImage(posterUrl)
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
