package de.gruetzmann.movie100

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gruetzmann.movie100.managers.MovieManager
import de.gruetzmann.movie100.viewmodel.Movie
import kotlinx.coroutines.launch

const val threshold = 200f

@Composable
fun MovieListComposable(login :String, modifier :Modifier =Modifier) {
    val movieManager = MovieManager
    val mainScope = rememberCoroutineScope()
    var movies by remember { mutableStateOf(listOf<Movie>()) }
    val listState = rememberLazyListState()

    LazyColumn(state= listState, modifier= modifier.padding(10.dp).background(MaterialTheme.colorScheme.background)) {
        itemsIndexed(movies, key= { _, movie -> movie.id }) { index, movie ->
            var dragOffsetY by remember { mutableFloatStateOf(0f) }

            Row(
                modifier= Modifier
                    .fillMaxWidth()
                    .padding(vertical= 4.dp),
                verticalAlignment= Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment= Alignment.CenterHorizontally,
                    verticalArrangement= Arrangement.Center,
                    modifier= Modifier.padding(end = 8.dp)
                ) {
                    Text("${index + 1}")
                    IconButton(
                        onClick= {
                            movieManager.reorderMovies(index, index-10)
                            mainScope.launch { listState.animateScrollToItem(index-10) }
                        },
                        enabled= index >= 10,
                        modifier= Modifier.height(28.dp).width(28.dp)
                    ) {
                        Text("M", fontSize= 14.sp)
                    }
                    IconButton(
                        onClick= { movieManager.moveMovieUp(index) },
                        enabled= index > 0,
                        modifier= Modifier.height(28.dp).width(28.dp)
                    ) {
                        Text("▲", fontSize= 14.sp)
                    }
                    Text("≡", fontSize= 18.sp,
                        modifier= Modifier
                            .pointerInput(index, movies.size) {
                                detectDragGestures(
                                    onDragStart= { dragOffsetY = 0f },
                                    onDragEnd= { dragOffsetY = 0f },
                                    onDragCancel= { dragOffsetY = 0f },
                                    onDrag= { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        if (dragOffsetY > threshold && index < movies.size - 1) {
                                            movieManager.moveMovieDown(index)
                                            dragOffsetY = 0f
                                        } else if (dragOffsetY < -threshold && index > 0) {
                                            movieManager.moveMovieUp(index)
                                            dragOffsetY = 0f
                                        }
                                    }
                                )
                            }
                            .padding(vertical= 2.dp)
                    )
                    IconButton(
                        onClick= { movieManager.moveMovieDown(index) },
                        enabled= index < movies.size - 1,
                        modifier= Modifier.height(28.dp).width(28.dp)
                    ) {
                        Text("▼", fontSize= 14.sp)
                    }
                    IconButton(
                        onClick= {
                            movieManager.reorderMovies(index, index+10)
                            mainScope.launch { listState.animateScrollToItem(index+10) }
                        },
                        enabled= index +10 < movies.size,
                        modifier= Modifier.height(28.dp).width(28.dp)
                    ) {
                        Text("W", fontSize= 14.sp)
                    }
                }
                val poster = movie.poster
                if (poster != null)
                    Image(poster, movie.title, modifier = Modifier.width(80.dp).height(120.dp).padding(end = 8.dp))

                Column(modifier= Modifier.weight(1f)) {
                    Row(modifier= Modifier.fillMaxWidth()) {
                        Text(
                            "${movie.title} *${movie.inceptionYear}",
                            fontSize= MaterialTheme.typography.titleLarge.fontSize,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick= { movieManager.removeUserMovie(movie) }, enabled= true,
                            colors = ButtonColors(Color.White, Color.Red, Color.Red, Color.Gray),
                            modifier= Modifier.width(32.dp).padding(0.dp)
                        ) {
                            Text("X", fontSize= 16.sp, color= Color.Red, modifier= Modifier
                                .padding(0.dp))
                        }
                    }
                    if (movie.director != null)
                        Text("director: ${movie.director}")
                    Column {
                        Text("with")
                        for (actor in movie.actors) {
                            Text("* $actor")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
        }
    }

    LaunchedEffect(login) {
        movieManager.subscribeUsersMovies(login) { newMovies :List<Movie> ->
            movies = newMovies
        }
    }
}
