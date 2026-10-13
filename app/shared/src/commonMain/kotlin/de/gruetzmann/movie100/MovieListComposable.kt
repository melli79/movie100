package de.gruetzmann.movie100

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.gruetzmann.movie100.managers.MovieManager
import de.gruetzmann.movie100.viewmodel.Movie

@Composable
fun MovieListComposable(login :String, modifier :Modifier = Modifier.fillMaxSize()) {
    val movieManager = MovieManager
    var movies by remember { mutableStateOf(listOf<Movie>()) }
    LazyColumn(modifier = modifier.padding(10.dp).background(MaterialTheme.colorScheme.background)) {
        items(movies) { movie :Movie ->
            Row(modifier = Modifier.fillMaxWidth()) {
                if (movie.poster != null)
                    Image(movie.poster, movie.name)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${movie.name} *${movie.inceptionYear}",
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    )
                    if (movie.director != null)
                        Text("director: ${movie.director}")
                    Column {
                        Text("with")
                        for (actor in movie.actors) {
                            Text("* ${actor}")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
        }
    }
    LaunchedEffect(Unit) {
        movies = movieManager.getUsersMovies(login)
    }
}