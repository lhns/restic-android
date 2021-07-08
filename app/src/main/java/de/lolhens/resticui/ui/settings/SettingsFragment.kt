package de.lolhens.resticui.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.lolhens.resticui.Backup
import de.lolhens.resticui.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _backup: Backup? = null
    private val backup get() = _backup!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _backup = Backup.instance(requireContext())

        binding.buttonUnlock.setOnClickListener {
            backup.config.repos.forEach { repo ->
                val resticRepo = repo.repo(backup.restic)
                resticRepo.unlock()
                    .handle { message, throwable ->
                        if (throwable != null) {
                            throwable.printStackTrace()
                        } else {
                            println(message)
                        }
                    }
            }
        }

        binding.buttonCleanup.setOnClickListener {
            backup.restic.restic(listOf("cache", "--cleanup"))
                .handle { message, throwable ->
                    if (throwable != null) {
                        throwable.printStackTrace()
                    } else {
                        println(message)
                    }
                }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _backup = null
        _binding = null
    }
}