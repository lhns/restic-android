package de.lolhens.resticui.ui.repos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.lolhens.resticui.databinding.FragmentReposBinding
import de.lolhens.resticui.ui.repo.RepoActivity

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

        val textView: TextView = binding.textRepos
        reposViewModel.text.observe(viewLifecycleOwner, {
            textView.text = it
        })

        binding.fabReposAdd.setOnClickListener { view ->
            Toast.makeText(requireContext(), "Added repo", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), RepoActivity::class.java)
            intent.putExtra("edit", true)
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}