package de.lolhens.resticui.ui.folder

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import de.lolhens.resticui.databinding.FragmentFolderBinding

class FolderFragment : Fragment() {
    private lateinit var folderViewModel: FolderViewModel
    private var _binding: FragmentFolderBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        folderViewModel =
            ViewModelProvider(this).get(FolderViewModel::class.java)

        _binding = FragmentFolderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        val textView: TextView = binding.textFolder
        folderViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(de.lolhens.resticui.R.menu.nav_menu_entry, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        val context = requireContext()
        when {
            id == de.lolhens.resticui.R.id.action_delete ->
                Toast.makeText(context, "Delete", Toast.LENGTH_SHORT).show()

            id == de.lolhens.resticui.R.id.action_edit ->
                Toast.makeText(context, "Edit", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}