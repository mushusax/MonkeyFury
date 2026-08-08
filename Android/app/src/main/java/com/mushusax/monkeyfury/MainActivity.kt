package com.mushusax.monkeyfury

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mushusax.monkeyfury.ui.main.MainActivityViewModel
import com.mushusax.monkeyfury.ui.screen.DashboardScreen
import com.mushusax.monkeyfury.ui.screen.LoginScreen
import com.mushusax.monkeyfury.ui.screen.TasksScreen
import com.mushusax.monkeyfury.ui.theme.MonkeyFuryTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    @Serializable
    object Login

    @Serializable
    object Dashboard

    @Serializable
    object Tasks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val focusManager = LocalFocusManager.current
            val softwareKeyboardController = LocalSoftwareKeyboardController.current
            navController.addOnDestinationChangedListener { controller, destination, arguments ->
                focusManager.clearFocus()
                softwareKeyboardController?.hide()
            }
            MonkeyFuryTheme {
                NavHost(navController = navController, startDestination = Login) {
                    composable<Login> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            LoginScreen(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .imePadding(),
                                onLogin = { user ->
                                    viewModel.user = user
                                    navController.navigate(route = Dashboard)
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
                                //viewModel.user = null
                            }

                            val onTasksClicked: () -> Unit = {
                                navController.navigate(route = Tasks)
                            }

                            DashboardScreen(
                                modifier = Modifier
                                    .fillMaxSize(),
                                user = viewModel.user,
                                onLogout = onLogout,
                                onTasksClicked = onTasksClicked
                            )
                        }
                    }
                    composable<Tasks> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            TasksScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .imePadding(),
                            )
                        }
                    }
                }

            }
        }
    }
}