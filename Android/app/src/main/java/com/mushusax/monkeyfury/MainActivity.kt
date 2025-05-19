package com.mushusax.monkeyfury

import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mushusax.monkeyfury.ui.theme.MonkeyFuryTheme
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonkeyFuryTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(snackbarHostState)
                    }
                ) { padding ->
                    LoginComposable(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        snackbarHostState,
                        application as MonkeyFuryApplication
                    )
                }
            }
        }
    }
}


@Composable
fun LoginComposable(
    modifier: Modifier,
    snackbarHostState: SnackbarHostState,
    application: MonkeyFuryApplication
) {

    Column(modifier, verticalArrangement = Arrangement.Center) {
        var enabled by rememberSaveable { mutableStateOf(true) }
        val coroutineScope = rememberCoroutineScope()

        ElevatedButton(
            onClick = {
                coroutineScope.launch {
                    enabled = false
                    snackbarHostState.showSnackbar("Log In Pressed")
                    enabled = true

                }
                application.weatherService.getWeather()
                    .enqueue(object : retrofit2.Callback<ResponseBody> {
                        override fun onResponse(
                            p0: Call<ResponseBody>,
                            p1: Response<ResponseBody>
                        ) {
                            Log.d("PTT", "onResponse")
                        }

                        override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {
                            Log.d("PTT", "onFailure")
                        }

                    })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        ) {
            Text(text = "Login", modifier = Modifier)
        }
    }
}