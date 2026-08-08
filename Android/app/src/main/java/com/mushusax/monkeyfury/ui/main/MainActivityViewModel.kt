package com.mushusax.monkeyfury.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi

class MainActivityViewModel(application: Application): AndroidViewModel(application) {
    var user: MonkeyFuryApi.User? = null
}