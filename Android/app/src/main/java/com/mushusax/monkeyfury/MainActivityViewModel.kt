package com.mushusax.monkeyfury

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainActivityViewModel(application: Application): AndroidViewModel(application) {
    var user: MonkeyFuryApi.User? = null
}