package de.lolhens.resticui.ui.repos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReposViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is repos Fragment"
    }
    val text: LiveData<String> = _text
}