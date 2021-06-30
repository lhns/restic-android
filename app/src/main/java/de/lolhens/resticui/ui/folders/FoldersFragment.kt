package de.lolhens.resticui.ui.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.databinding.FragmentFoldersBinding
import de.lolhens.resticui.ui.folder.FolderActivity

class FoldersFragment : Fragment() {
    private var _binding: FragmentFoldersBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        MainActivity.instance.observeConfig(viewLifecycleOwner) { config ->
            binding.listFolders.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                config.folders.map { "${it.repo(config)?.base?.name} ${it.path.path}" }
            )
        }

        binding.fabFoldersAdd.setOnClickListener { _ ->
            FolderActivity.start(this, true, FolderConfigId.create())
        }

        binding.listFolders.setOnItemClickListener { _, _, position, _ ->
            val folder = MainActivity.instance.config.folders.get(position)
            FolderActivity.start(this, false, folder.id)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}