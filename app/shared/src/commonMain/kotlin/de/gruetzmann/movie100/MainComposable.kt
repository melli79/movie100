package de.gruetzmann.movie100

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gruetzmann.movie100.managers.MovieManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainComposable(login :String, logout:()->Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog)
        DeleteDialog(login, logout) { showDeleteDialog = false }
    Column(
        modifier= Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment= Alignment.CenterHorizontally,
    ) {
        Row(modifier= Modifier.fillMaxWidth()) {
            Spacer(modifier= Modifier.weight(1f))
            Text("Hi $login!")
            Spacer(modifier= Modifier.weight(1f))
            Button(onClick= { logout() }) {
                Text("Logout")
            }
            Button(colors= ButtonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceDim),
                onClick= { showDeleteDialog = true }
            ) {
                Text("X Delete User data", color= MaterialTheme.colorScheme.error)
            }
        }
        MovieSearchBar(login)
        MovieListComposable(login, modifier= Modifier.fillMaxSize())
    }
}

@Composable
private fun DeleteDialog(login :String, logout :()->Unit, dismiss :()->Unit) {
    val manager = MovieManager
    val coroutineScope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest= dismiss,
        title= { Text("Delete User Data") },
        text= { Text("Are you sure you want to delete all user data for $login? This action cannot be undone.") },
        confirmButton= {
            TextButton(modifier= Modifier.background(MaterialTheme.colorScheme.errorContainer), onClick= {
                coroutineScope.launch {
                    try {
                        manager.deleteUser("DELETE")
                    } catch (e :Exception) {
                        e.printStackTrace()
                    }
                    logout()
                }
                dismiss()
            }) {
                Text("Delete", color= MaterialTheme.colorScheme.error)
            }
        },
        dismissButton= {
            TextButton(modifier= Modifier.background(MaterialTheme.colorScheme.secondaryContainer), onClick= dismiss) {
                Text("Cancel")
            }
        }
    )
}
