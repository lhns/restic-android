package de.lolhens.resticui.ui.repo

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.lolhens.resticui.databinding.FragmentRepoBinding

class RepoFragment : Fragment() {
    private lateinit var repoViewModel: RepoViewModel
    private var _binding: FragmentRepoBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        repoViewModel =
            ViewModelProvider(this).get(RepoViewModel::class.java)

        _binding = FragmentRepoBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        val textView: TextView = binding.repoTextRepo
        repoViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(de.lolhens.resticui.R.menu.nav_menu_entry, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.getItemId()) {
            de.lolhens.resticui.R.id.action_delete -> {
                Toast.makeText(context, "Delete", Toast.LENGTH_SHORT).show()
                true
            }
            de.lolhens.resticui.R.id.action_edit -> {
                Toast.makeText(context, "Edit", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}