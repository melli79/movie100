package de.gruetzmann.movie100

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gruetzmann.movie100.managers.MovieManager
import de.gruetzmann.movie100.viewmodel.Movie

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MovieSearchBar(login :String) {
    val movieManager = MovieManager
    var movieName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var movieCandidates by remember { mutableStateOf(listOf<Movie>()) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Next movie: ")
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = movieName,
                    onQueryChange = { newValue ->
                        movieName = newValue
                        if (movieName.isNotBlank()) {
                            movieCandidates = movieManager.findByNameLike(movieName.trim())
                            expanded = movieCandidates.isNotEmpty()
                        } else {
                            movieCandidates = emptyList()
                            expanded = false
                        }
                    },
                    onSearch = { },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Find your movie") },
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            for (movie in movieCandidates) {
                Button(modifier= Modifier.fillMaxWidth(), shape= RoundedCornerShape(10.dp),
                    onClick= {
                        if (movieManager.addMovie(login, movie)) {
                            expanded = false
                            movieName = ""
                        }
                    }
                ) {
                    Text(movie.toString(), modifier= Modifier.padding(10.dp).fillMaxWidth())
                }
            }
        }
    }
}
