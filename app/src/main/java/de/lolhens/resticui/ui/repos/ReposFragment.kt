package de.lolhens.resticui.ui.repos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import de.lolhens.resticui.databinding.FragmentReposBinding

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
        reposViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}