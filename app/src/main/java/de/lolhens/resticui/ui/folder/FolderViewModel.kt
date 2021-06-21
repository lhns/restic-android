package de.lolhens.resticui.ui.folder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FolderViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is folder Fragment"
    }
    val text: LiveData<String> = _text
}