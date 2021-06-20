package de.lolhens.resticui.ui.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AboutViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = """
            Restic UI
            by LolHens
            
            https://github.com/restic/restic
            https://github.com/termux/proot
        """.trimIndent()
    }
    val text: LiveData<String> = _text
}