package com.mushusax.monkeyfury

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginComposable(
    modifier: Modifier,
    application: MonkeyFuryApplication
) {

    Column(modifier, verticalArrangement = Arrangement.Center) {
        var enableLogin by rememberSaveable { mutableStateOf(true) }
        var enableLogout by rememberSaveable { mutableStateOf(true) }
        val coroutineScope = rememberCoroutineScope()

        ElevatedButton(
            onClick = {
                enableLogin = false
                application.api.login()
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(
                            p0: Call<ResponseBody>,
                            p1: Response<ResponseBody>
                        ) {
                            coroutineScope.launch {
                                enableLogin = true
                            }
                        }

                        override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {
                            coroutineScope.launch {
                                enableLogin = true
                            }
                        }
                    })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enableLogin
        ) {
            Text(text = "Log in", modifier = Modifier)
        }

        ElevatedButton(
            onClick = {
                enableLogout = true
                application.api.logout()
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(
                            p0: Call<ResponseBody>,
                            p1: Response<ResponseBody>
                        ) {
                            coroutineScope.launch {
                                enableLogout = true
                            }
                        }

                        override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {
                            coroutineScope.launch {
                                enableLogout = true
                            }
                        }

                    })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enableLogout
        ) {
            Text(text = "Log out", modifier = Modifier)
        }
    }
}