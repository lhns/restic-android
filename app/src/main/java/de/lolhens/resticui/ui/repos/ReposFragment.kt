package de.lolhens.resticui.ui.repos

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.databinding.FragmentReposBinding
import de.lolhens.resticui.ui.repo.RepoActivity
import java.util.*

class ReposFragment : Fragment() {

    private lateinit var reposViewModel: ReposViewModel
    private var _binding: FragmentReposBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        reposViewModel =
            ViewModelProvider(this).get(ReposViewModel::class.java)

        _binding = FragmentReposBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.fabReposAdd.setOnClickListener { view ->
            Toast.makeText(requireContext(), "Added repo", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), RepoActivity::class.java)
            intent.putExtra("edit", true)
            intent.putExtra("id", UUID.randomUUID().toString())
            startActivityForResult(intent, 0)
        }

        MainActivity.instance.config.observe(viewLifecycleOwner) { config ->
            binding.listViewRepos.adapter = ArrayAdapter(
                requireContext(),
                R.layout.simple_list_item_1,
                config.repos.map { it.base.name }
            )
        }

        binding.listViewRepos.setOnItemClickListener { parent, view, position, id ->
            val repo = MainActivity.instance.config.value?.repos?.get(position)
            if (repo != null) {
                Toast.makeText(
                    requireContext(),
                    "Clicked repo: ${repo.base.name}",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(requireContext(), RepoActivity::class.java)
                intent.putExtra("edit", true) // TODO false
                intent.putExtra("id", repo.base.id.uuid.toString())
                startActivityForResult(intent, 0)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}