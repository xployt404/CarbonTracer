package com.example.carbontracerrevised

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _sharedString = MutableLiveData<String>()
    val sharedString: LiveData<String> get() = _sharedString

    fun setString(data: String) {
        _sharedString.value = data
    }
}
