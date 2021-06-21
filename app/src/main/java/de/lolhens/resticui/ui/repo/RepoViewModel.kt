package de.lolhens.resticui.ui.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RepoViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is repo Fragment"
    }
    val text: LiveData<String> = _text
}