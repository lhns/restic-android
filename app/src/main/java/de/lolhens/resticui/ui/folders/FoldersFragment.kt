package de.lolhens.resticui.ui.folders

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.lolhens.resticui.Permissions
import de.lolhens.resticui.databinding.FragmentFoldersBinding
import de.lolhens.resticui.ui.folder.FolderActivity

class FoldersFragment : Fragment() {

    private lateinit var foldersViewModel: FoldersViewModel
    private var _binding: FragmentFoldersBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foldersViewModel =
            ViewModelProvider(this).get(FoldersViewModel::class.java)

        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        Permissions.request(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
            .thenApply { granted ->
                if (granted) {
                    val extStorageDir = Environment.getExternalStorageDirectory()

                    val myArrayAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        extStorageDir.list()!!
                    )
                    binding.listViewFolders.adapter = myArrayAdapter
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Until you grant the permission, I cannot list the files",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }

        binding.listViewFolders.setOnItemClickListener { parent, view, position, id ->
            Toast.makeText(requireContext(), "Clicked item: $position", Toast.LENGTH_SHORT).show()

            /*val folderFragment = FolderFragment()
            parentFragmentManager.commit {
                replace(R.id.nav_host_fragment_activity_main, folderFragment)
                addToBackStack(null)
            }*/

            val intent = Intent(requireContext(), FolderActivity::class.java)
            startActivity(intent)
        }

        binding.fabFoldersAdd.setOnClickListener { view ->
            Toast.makeText(requireContext(), "Added folder", Toast.LENGTH_SHORT).show()
        }

        /*foldersViewModel.text.observe(viewLifecycleOwner, Observer {
            textView = it
        })*/
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}