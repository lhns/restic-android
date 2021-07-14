package de.lolhens.resticui.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.lolhens.resticui.Backup
import de.lolhens.resticui.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private var _binding: FragmentAboutBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _backup: Backup? = null
    private val backup get() = _backup!!

    private fun aboutText(resticVersion: String? = null) = resticVersion ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _backup = Backup.instance(requireContext())

        val restic = backup.restic

        val textView: TextView = binding.textResticVersion
        textView.text = aboutText()
        restic.version()
            .thenAccept { resticVersion ->
                requireActivity().runOnUiThread {
                    textView.text = aboutText(resticVersion)
                }
            }
            .exceptionally {
                it.printStackTrace()
                null
            }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _backup = null
        _binding = null
    }
}