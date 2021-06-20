package de.lolhens.resticui.ui.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FoldersViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is folders Fragment"
    }
    val text: LiveData<String> = _text
}