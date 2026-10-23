package de.gruetzmann.movie100

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gruetzmann.movie100.managers.MovieManager
import de.gruetzmann.movie100.viewmodel.Movie
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MovieSearchBar(login :String) {
    val movieManager = MovieManager
    val coroutineScope = rememberCoroutineScope()
    var movieName by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
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
                        searchJob?.cancel()
                        if (movieName.isNotBlank()) searchJob = coroutineScope.launch {
                            delay(1000.milliseconds)
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
                    trailingIcon = {
                        if (movieName.isNotEmpty()) {
                            IconButton(onClick = { movieName = "" }) {
                                Text("✕", fontSize = 14.sp)
                            }
                        }
                    }

                )
            },
            expanded= expanded,
            onExpandedChange= { expanded = it },
        ) {
            for (movie in movieCandidates) {
                Button(modifier= Modifier.fillMaxWidth(), shape= RoundedCornerShape(10.dp),
                    onClick= {
                        if (movieManager.addMovie(movie)) {
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
