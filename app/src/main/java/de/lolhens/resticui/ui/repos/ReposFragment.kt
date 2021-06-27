package de.lolhens.resticui.ui.repos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.config.RepoConfigId
import de.lolhens.resticui.databinding.FragmentReposBinding
import de.lolhens.resticui.ui.repo.RepoActivity

class ReposFragment : Fragment() {
    private var _binding: FragmentReposBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReposBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.fabReposAdd.setOnClickListener { view ->
            RepoActivity.start(this, true, RepoConfigId.create())
        }

        MainActivity.instance.observeConfig(viewLifecycleOwner) { config ->
            binding.listRepos.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                config.repos.map { it.base.name }
            )
        }

        binding.listRepos.setOnItemClickListener { parent, view, position, id ->
            val repo = MainActivity.instance.config.repos.get(position)
            RepoActivity.start(this, false, repo.base.id)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}