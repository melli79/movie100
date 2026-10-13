package de.gruetzmann.movie100

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(true) }
        var login by remember { mutableStateOf("melli") }
        var password by remember { mutableStateOf("") }
        Column(modifier= Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
            horizontalAlignment= Alignment.CenterHorizontally,
        ) {
            if (showContent) {
                MainComposable(login)
            } else {
                Row(modifier= Modifier.fillMaxWidth(), verticalAlignment= Alignment.CenterVertically) {
                    Text("Login: ")
                    TextField(value = login, onValueChange = { login = it }, singleLine= true)
                }
                Spacer(modifier= Modifier.height(16.dp))
                Row(modifier= Modifier.fillMaxWidth(), verticalAlignment= Alignment.CenterVertically) {
                    Text("Password: ")
                    TextField(value= password, onValueChange= { password = it }, singleLine= true,
                        visualTransformation= PasswordVisualTransformation()
                    )
                }
                Spacer(modifier= Modifier.height(16.dp))
                Row(modifier= Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        if (login.isNotBlank() && password.isNotBlank())
                           showContent = true
                    }, enabled = login.isNotBlank() && password.isNotBlank()) {
                        Text("Enter")
                    }
                }
                Spacer(modifier= Modifier.weight(1f))
            }
        }
    }
}
