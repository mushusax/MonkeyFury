package com.mushusax.monkeyfury

import androidx.compose.foundation.Image
import androidx.compose.foundation.content.MediaType.Companion.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mushusax.monkeyfury.ui.theme.MonkeyFuryTheme
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

        Image(
            painter = painterResource(R.drawable.monkeyfury),
            contentDescription = "Monkey Fury",
            modifier = Modifier.fillMaxWidth(1f)
        )

        Spacer(modifier = Modifier.weight(1f))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {

            var email by remember { mutableStateOf("Email") }
            var password by remember { mutableStateOf("Password") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email".uppercase()) },
                    modifier = Modifier.fillMaxWidth(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password".uppercase()) },
                    modifier = Modifier.fillMaxWidth(1f),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Button(
                    shape = RoundedCornerShape(2.dp),
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
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(vertical = 2.dp),
                    enabled = enableLogin
                ) {
                    Text(text = "Log in".uppercase(), modifier = Modifier)
                }

                TextButton(onClick = {}, modifier = Modifier.fillMaxWidth(1f)) {
                    Text(text = "Forgot Password".uppercase(), modifier = Modifier)
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth(1f),
                    thickness = 2.dp
                )


            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MonkeyFuryTheme {
        LoginComposable(modifier = Modifier.fillMaxSize(), MonkeyFuryApplication())
    }
}