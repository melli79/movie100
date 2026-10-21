package de.gruetzmann.movie100

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.gruetzmann.movie100.managers.MovieManager
import kotlinx.coroutines.launch

@Composable
fun MainComposable(login :String, logout:()->Unit) {
    val manager = MovieManager
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier= Modifier.fillMaxWidth()) {
            Spacer(modifier= Modifier.weight(1f))
            Text("Hi $login!")
            Spacer(modifier= Modifier.weight(1f))
            Button(onClick = { logout() }) {
                Text("Logout")
            }
            Button(colors= ButtonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceDim),
                onClick = { coroutineScope.launch {
                    try {
                        manager.deleteUser()
                    } catch (e :Exception) {
                        e.printStackTrace()
                    }
                    logout()
                } }
            ) {
                Text("X Delete User data", color= MaterialTheme.colorScheme.error)
            }
        }
        MovieSearchBar(login)
        MovieListComposable(login, modifier= Modifier.fillMaxSize())
    }
}
