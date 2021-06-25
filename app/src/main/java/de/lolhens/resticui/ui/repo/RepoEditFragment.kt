package de.lolhens.resticui.ui.repo

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.R
import de.lolhens.resticui.config.*
import de.lolhens.resticui.databinding.FragmentRepoEditBinding
import java.net.URI

class RepoEditFragment : Fragment() {
    private var _binding: FragmentRepoEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _repoId: RepoConfigId? = null
    private val repoId: RepoConfigId? get() = _repoId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRepoEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        val repoActivity = requireActivity() as RepoActivity
        _repoId = repoActivity.repoId
        val repo =
            if (repoId == null) null
            else MainActivity.instance.config.value?.repos?.find { it.base.id == repoId }

        if (repo != null) {
            binding.editRepoName.setText(repo.base.name)
            binding.editRepoPassword.setText(repo.base.password)
            val s3RepoParams = repo.params as S3RepoParams
            binding.editS3Url.setText(s3RepoParams.s3Url.toString())
            binding.editAccessKeyId.setText(s3RepoParams.accessKeyId)
            binding.editSecretAccessKey.setText(s3RepoParams.secretAccessKey)
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.getItemId()) {
            R.id.action_done -> {
                val repoName = binding.editRepoName.text.toString()
                val repoPassword = binding.editRepoPassword.text.toString()
                val s3UrlString = binding.editS3Url.text.toString()
                val accessKeyId = binding.editAccessKeyId.text.toString()
                val secretAccessKey = binding.editSecretAccessKey.text.toString()

                if (
                    repoName.length > 0 &&
                    s3UrlString.length > 0
                ) {
                    val repo = RepoConfig(
                        RepoBaseConfig.create(
                            repoName,
                            RepoType.S3,
                            repoPassword
                        ),
                        S3RepoParams(
                            URI(s3UrlString),
                            accessKeyId,
                            secretAccessKey
                        )
                    )

                    MainActivity.instance.configure { config ->
                        config.copy(repos = config.repos.filterNot { it.base.id == repoId }
                            .plus(repo))
                    }

                    requireActivity().finish()
                    true
                } else
                    false
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}