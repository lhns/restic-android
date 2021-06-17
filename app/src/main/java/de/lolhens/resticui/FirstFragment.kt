package de.lolhens.resticui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import de.lolhens.resticui.databinding.FragmentFirstBinding
import java.net.URI

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    private val binding get() = _binding!!

    private var _restic: Restic? = null

    private val restic get() = _restic!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        _restic = Restic(ResticStorage.fromContext(requireContext()))

        restic.version().thenAccept { binding.textViewVersion.setText(it) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonLogin.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)

            val repo = ResticRepoS3(
                restic,
            )

            repo.snapshots().thenAccept { result ->
                println(result.joinToString("\n"))

                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }.exceptionally {
                it.printStackTrace()
                null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _restic = null
        _binding = null
    }
}