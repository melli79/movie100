package de.gruetzmann.movie100.model.inits

import de.gruetzmann.movie100.model.ActorDto
import de.gruetzmann.movie100.model.Gender
import de.gruetzmann.movie100.model.MovieDto
import de.gruetzmann.movie100.model.Url
import kotlin.time.Instant

val knownMovies = listOf(
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
        "Peter Jackson", Instant.parse("2001-12-01T00:00:00Z")),
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
