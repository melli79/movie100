package de.gruetzmann.movie100.managers

import de.gruetzmann.movie100.viewmodel.Movie
import ulid.ULID
import kotlin.random.Random
import kotlin.time.Clock.System.now

object MovieManager {
    private val ulidFactory = ULID.Factory(random= Random(now().toEpochMilliseconds()))
    private var movies = mutableListOf(
        Movie(ulidFactory.nextULID(), "Star Trek IX First Contact", listOf("Patrick Steward", "Brent Spiner", "Jonathan Frakes", "LeVar Burton"), 1996),
        Movie(ulidFactory.nextULID(), "The Lord of the Rings I The Fellowship", listOf("Elija Wood", "Ian McKellen", "Orlando Bloom"), 2001),
        Movie(ulidFactory.nextULID(), "The Godfather", listOf("Marlon Brando", "Al Pachino"), 1972),
        Movie(ulidFactory.nextULID(), "The Green Butcher", listOf("Nikolaj Lie Kaas", "Mads Mikkelsen", "Line Kruse"), 2003),
    )
    private var usersMovies = movies.take(3).toMutableList()

    fun findByNameLike(pattern :String) :List<Movie> = movies.filter { it.name.contains(pattern) }
    fun getUsersMovies(user :String) :List<Movie> {
        return usersMovies
    }

    fun addMovie(login :String, movie :Movie) {
        if (movie !in usersMovies) {
            usersMovies.add(movie)
        }
    }
}
