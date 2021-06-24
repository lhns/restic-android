package de.lolhens.resticui.ui.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.lolhens.resticui.config.Config
import de.lolhens.resticui.config.RepoConfig
import de.lolhens.resticui.restic.Restic
import java.io.File

class FoldersViewModel(config: Config, restic: Restic) : ViewModel() {
    private val _list = MutableLiveData<List<Pair<File, RepoConfig>>>().apply {
        value = config.directories
    }

    val list: LiveData<List<Pair<File, RepoConfig>>> = _list
}