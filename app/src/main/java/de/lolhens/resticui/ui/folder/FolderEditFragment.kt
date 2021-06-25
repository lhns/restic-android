package de.lolhens.resticui.ui.folder

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.lolhens.resticui.R
import de.lolhens.resticui.databinding.FragmentFolderEditBinding

class FolderEditFragment : Fragment() {
    private var _binding: FragmentFolderEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFolderEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        val textView: TextView = binding.folderEditTextFolder

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.getItemId()) {
            R.id.action_done -> {
                Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}