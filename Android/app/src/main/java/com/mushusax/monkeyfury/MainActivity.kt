package com.mushusax.monkeyfury

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mushusax.monkeyfury.ui.theme.MonkeyFuryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonkeyFuryTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { padding ->
                    LoginComposable(modifier = Modifier
                        .padding(padding)
                        .fillMaxSize())
                }
            }
        }
    }
}


@Composable
fun LoginComposable(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        ElevatedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Login", modifier = Modifier)
        }
    }
}