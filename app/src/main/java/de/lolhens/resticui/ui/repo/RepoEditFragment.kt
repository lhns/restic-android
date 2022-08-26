package de.lolhens.resticui.ui.repo

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.View.*
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import de.lolhens.resticui.BackupManager
import de.lolhens.resticui.R
import de.lolhens.resticui.config.*
import de.lolhens.resticui.databinding.FragmentRepoEditBinding
import java.net.URI
import java.util.concurrent.CompletionException


class RepoEditFragment : Fragment() {
    private var _binding: FragmentRepoEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _backupManager: BackupManager? = null
    private val backupManager get() = _backupManager!!

    private lateinit var _repoId: RepoConfigId
    private val repoId: RepoConfigId get() = _repoId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRepoEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _backupManager = BackupManager.instance(requireContext())

        _repoId = (requireActivity() as RepoActivity).repoId
        val repo = backupManager.config.repos.find { it.base.id == repoId }

        // define a listener to change the repo param view based on which repo type is selected in the drop down
        binding.spinnerRepoType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val getRepoTypeBinding: (RepoType) -> ViewBinding = {
                    when (it) {
                        RepoType.S3 -> binding.editRepoS3Parameters
                        RepoType.Rest -> binding.editRepoRestParameters
                        RepoType.B2 -> binding.editRepoB2Parameters
                    }
                }

                val newRepoType = RepoType.valueOf(parent!!.getItemAtPosition(position).toString())
                RepoType.values().forEach { repoType ->
                    getRepoTypeBinding(repoType).root.visibility =
                        if (repoType == newRepoType) VISIBLE else GONE
                }
            }
        }

        if (repo != null) {
            // Disable Spinner in Edit mode since it should not be possible to change a repos type after is has been
            // created
            binding.spinnerRepoType.isEnabled = false
            binding.spinnerRepoType.isClickable = false

            // prefill the view if the repo already exists and is going to be edited instead of created.
            binding.editRepoName.setText(repo.base.name)
            binding.editRepoPassword.setText(repo.base.password.secret)
            binding.spinnerRepoType.setSelection(RepoType.values().indexOf(repo.base.type))
            when (repo.base.type) {
                RepoType.S3 -> {
                    val s3RepoParams = repo.params as S3RepoParams
                    binding.editRepoS3Parameters.editS3Uri.setText(s3RepoParams.s3Url.toString())
                    binding.editRepoS3Parameters.editS3AccessKeyId.setText(s3RepoParams.accessKeyId)
                    binding.editRepoS3Parameters.editS3SecretAccessKey.setText(s3RepoParams.secretAccessKey.secret)
                }
                RepoType.Rest -> {
                    val restRepoParams = repo.params as RestRepoParams
                    binding.editRepoRestParameters.editRestUri.setText(restRepoParams.restUrl.toString())
                }
                RepoType.B2 -> {
                    val b2RepoParams = repo.params as B2RepoParams
                    binding.editRepoB2Parameters.editB2Uri.setText(b2RepoParams.b2Url.toString())
                    binding.editRepoB2Parameters.editB2AccountId.setText(b2RepoParams.b2AccountId)
                    binding.editRepoB2Parameters.editB2AccountKey.setText(b2RepoParams.b2AccountKey.secret)
                }

            }.apply {} // do not remove - throws a compiler error if any of the repo types cases is not covered by the when
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_done -> {
                val (valid, repo) = parseRepo()

                if (valid) {

                    fun saveRepo() {
                        backupManager.configure { config ->
                            config.copy(repos = config.repos.filterNot { it.base.id == repoId }
                                .plus(repo!!))
                        }

                        RepoActivity.start(this, false, repoId)

                        requireActivity().finish()
                    }

                    Toast.makeText(context, R.string.text_saving, Toast.LENGTH_SHORT).show()

                    item.isEnabled = false
                    binding.progressRepoSave.visibility = VISIBLE

                    val resticRepo = repo!!.repo(backupManager.restic)
                    resticRepo.stats().handle { _, throwable ->
                        requireActivity().runOnUiThread {
                            if (throwable == null) {
                                saveRepo()
                            } else {
                                System.err.println("Error saving repository!")
                                throwable.printStackTrace()

                                item.isEnabled = true
                                binding.progressRepoSave.visibility = INVISIBLE

                                AlertDialog.Builder(requireActivity())
                                    .setTitle(R.string.alert_init_repo_title)
                                    .setMessage(R.string.alert_init_repo_message)
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        item.isEnabled = false
                                        binding.progressRepoSave.visibility = VISIBLE

                                        resticRepo.init().handle { _, throwable ->
                                            requireActivity().runOnUiThread {
                                                if (throwable == null) {
                                                    saveRepo()
                                                } else {
                                                    val throwable =
                                                        if (throwable is CompletionException && throwable.cause != null) throwable.cause!!
                                                        else throwable

                                                    throwable.printStackTrace()

                                                    item.isEnabled = true
                                                    binding.progressRepoSave.visibility = INVISIBLE

                                                    AlertDialog.Builder(requireActivity())
                                                        .setTitle(R.string.alert_save_repo_title)
                                                        .setMessage(
                                                            "${
                                                                requireContext().resources.getString(
                                                                    R.string.alert_save_repo_message
                                                                )
                                                            }\n\n${throwable.message}\n\n${
                                                                requireContext().resources.getString(
                                                                    R.string.alert_save_repo_question
                                                                )
                                                            }"
                                                        )
                                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                                            saveRepo()
                                                        }
                                                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                                        .show()
                                                }
                                            }
                                        }
                                    }
                                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                    .show()
                            }
                        }
                    }

                    true
                } else {
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun parseRepo(): Pair<Boolean, RepoConfig?> {
        val repoType = RepoType.valueOf(binding.spinnerRepoType.selectedItem as String)
        val valid = validateRepo(repoType)

        if (!valid) {
            return false to null
        }

        val baseConfig = RepoBaseConfig(
            id = repoId,
            name = binding.editRepoName.text.toString(),
            type = repoType,
            password = Secret(binding.editRepoPassword.text.toString())
        )

        return true to when (repoType) {
            RepoType.S3 -> {
                RepoConfig(
                    baseConfig,
                    S3RepoParams(
                        s3Url = URI(binding.editRepoS3Parameters.editS3Uri.text.toString()),
                        accessKeyId = binding.editRepoS3Parameters.editS3AccessKeyId.text.toString(),
                        secretAccessKey = Secret(binding.editRepoS3Parameters.editS3SecretAccessKey.text.toString())
                    )
                )
            }
            RepoType.Rest -> {
                RepoConfig(
                    baseConfig,
                    RestRepoParams(
                        restUrl = URI(binding.editRepoRestParameters.editRestUri.text.toString()),
                    )
                )
            }
            RepoType.B2 -> {
                RepoConfig(
                    baseConfig,
                    B2RepoParams(
                        b2Url = URI(binding.editRepoB2Parameters.editB2Uri.text.toString()),
                        b2AccountId = binding.editRepoB2Parameters.editB2AccountId.text.toString(),
                        b2AccountKey = Secret(binding.editRepoB2Parameters.editB2AccountKey.text.toString()),
                    )
                )
            }
        }
    }

    private fun checkFieldMandatory(field: TextView, errorMessage: String): Boolean {
        if (field.text.toString().isEmpty()) {
            field.error = errorMessage
            return false
        }
        return true
    }

    private fun validateRepo(repoType: RepoType): Boolean {
        val baseValidatorResults = listOf(
            checkFieldMandatory(binding.editRepoName, getString(R.string.repo_edit_name_error_mandatory)),
            checkFieldMandatory(binding.editRepoPassword, getString(R.string.repo_edit_password_error_mandatory)),
        )

        val validatorResults = when (repoType) {
            RepoType.S3 -> {
                baseValidatorResults.plus(
                    listOf(
                        checkFieldMandatory(
                            binding.editRepoS3Parameters.editS3Uri,
                            getString(R.string.repo_edit_s3_uri_error_mandatory)
                        ),
                        checkFieldMandatory(
                            binding.editRepoS3Parameters.editS3AccessKeyId,
                            getString(R.string.repo_edit_s3_access_key_id_error_mandatory)
                        ),
                        checkFieldMandatory(
                            binding.editRepoS3Parameters.editS3SecretAccessKey,
                            getString(R.string.repo_edit_s3_secret_access_key_error_mandatory)
                        ),

                        )
                )
            }
            RepoType.Rest -> {
                baseValidatorResults.plus(
                    listOf(
                        checkFieldMandatory(
                            binding.editRepoRestParameters.editRestUri,
                            getString(R.string.repo_edit_rest_uri_error_mandatory)
                        ),

                        )
                )
            }
            RepoType.B2 -> {
                baseValidatorResults.plus(
                    listOf(
                        checkFieldMandatory(
                            binding.editRepoB2Parameters.editB2Uri,
                            getString(R.string.repo_edit_b2_uri_error_mandatory)
                        ),
                        checkFieldMandatory(
                            binding.editRepoB2Parameters.editB2AccountId,
                            getString(R.string.repo_edit_b2_account_id_error_mandatory)
                        ),
                        checkFieldMandatory(
                            binding.editRepoB2Parameters.editB2AccountKey,
                            getString(R.string.repo_edit_b2_account_key_error_mandatory)
                        ),

                        )
                )
            }
        }

        return validatorResults.all { result -> result }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _backupManager = null
        _binding = null
    }
}