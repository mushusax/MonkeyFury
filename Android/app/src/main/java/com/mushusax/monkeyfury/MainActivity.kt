package com.mushusax.monkeyfury

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mushusax.monkeyfury.ui.DashboardScreen
import com.mushusax.monkeyfury.ui.LoginScreen
import com.mushusax.monkeyfury.ui.theme.MonkeyFuryTheme
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    @Serializable
    object Login

    @Serializable
    object Dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val navController = rememberNavController()
            MonkeyFuryTheme {
                NavHost(navController = navController, startDestination = Login) {
                    composable<Login> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            LoginScreen(modifier = Modifier.fillMaxWidth(), onLogin = {
                                navController.navigate(Dashboard)
                            })
                        }
                    }
                    composable<Dashboard> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            val onLogout: () -> Unit = {
                                navController.popBackStack()
                            }

                            val onHealthCheck: () -> Unit = {
                                (application as MonkeyFuryApplication).api.healthCheck()
                                    .enqueue(object : Callback<ResponseBody> {
                                        override fun onResponse(
                                            call: Call<ResponseBody?>,
                                            response: Response<ResponseBody?>
                                        ) {
                                            println("Success")
                                        }

                                        override fun onFailure(
                                            call: Call<ResponseBody?>,
                                            t: Throwable
                                        ) {
                                            println("Failure")
                                        }
                                    })
                            }

                            DashboardScreen(
                                modifier = Modifier.fillMaxSize(),
                                onLogout = onLogout,
                                onHealthCheck = onHealthCheck
                            )
                        }
                    }
                }

            }
        }
    }
}