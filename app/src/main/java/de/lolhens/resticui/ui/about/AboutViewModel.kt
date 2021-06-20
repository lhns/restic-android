package de.lolhens.resticui.ui.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.lolhens.resticui.restic.Restic

class AboutViewModel(restic: Restic) : ViewModel() {
    private fun aboutText(resticVersion: String? = null) = """
            Restic UI
            by LolHens
            
            ${resticVersion ?: ""}
            
            https://github.com/restic/restic
            https://github.com/termux/proot
        """.trimIndent()

    private val _text = MutableLiveData<String>().apply {
        value = aboutText()
    }

    init {
        restic.version()
            .thenAccept { _text.postValue(aboutText(it)) }
            .exceptionally {
                it.printStackTrace()
                null
            }
    }

    val text: LiveData<String> = _text
}