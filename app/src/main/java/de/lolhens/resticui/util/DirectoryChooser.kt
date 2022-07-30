package de.lolhens.resticui.util

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

abstract class DirectoryChooser {
    abstract fun register(
        activityResultCaller: ActivityResultCaller,
        context: Context,
        onChoose: (String) -> Unit
    )

    abstract fun openDialog()

    companion object {
        fun newInstance() = DefaultDirectoryChooser()

        class DefaultDirectoryChooser : DirectoryChooser() {
            private lateinit var launcher: ActivityResultLauncher<Intent>

            override fun register(
                activityResultCaller: ActivityResultCaller,
                context: Context,
                onChoose: (String) -> Unit
            ) {
                launcher =
                    activityResultCaller.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        val uri = result.data?.data
                        if (uri != null) {
                            println("Selected directory: $uri")

                            val path =
                                FileUtil.getFullPathFromTreeUri(
                                    context,
                                    uri
                                ) ?: ASFUriHelper.getPath(
                                    context,
                                    DocumentsContract.buildDocumentUriUsingTree(
                                        uri,
                                        DocumentsContract.getTreeDocumentId(uri)
                                    )
                                )

                            onChoose(path)
                        }
                    }
            }

            override fun openDialog() {
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                i.addCategory(Intent.CATEGORY_DEFAULT)
                launcher.launch(Intent.createChooser(i, "Choose directory"))
            }
        }
    }
}