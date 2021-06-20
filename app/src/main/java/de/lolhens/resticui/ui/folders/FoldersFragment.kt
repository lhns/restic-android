package de.lolhens.resticui.ui.folders

import android.Manifest
import android.R
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import de.lolhens.resticui.ConfigManager
import de.lolhens.resticui.databinding.FragmentFoldersBinding
import java.io.File

class FoldersFragment : Fragment() {

    private lateinit var foldersViewModel: FoldersViewModel
    private var _binding: FragmentFoldersBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    val requestCode = 100

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == requestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted


            } else {
                Toast.makeText(requireContext(), "Until you grant the permission, I cannot list the files", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foldersViewModel =
            ViewModelProvider(this).get(FoldersViewModel::class.java)

        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (ConfigManager.checkPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE, requestCode)) {
            val extStorageDir = Environment.getExternalStorageDirectory()
            val state = Environment.getExternalStorageState()

            println(Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state)
            println(extStorageDir)
            println(extStorageDir.list().toList())

            val myArrayAdapter = ArrayAdapter<String>(
                requireContext(),
                R.layout.simple_list_item_1,
                extStorageDir.list()!!
            )
            binding.listViewFolders.adapter = myArrayAdapter
        }

        binding.listViewFolders.setOnItemClickListener { parent, view, position, id ->
            Toast.makeText(requireContext(), "Clicked item: $position",Toast.LENGTH_SHORT).show()

            /*val intent = Intent(this, NextActivity::class.java)
            intent.putExtra("position", position)
            this.startActivity(intent)*/
        }

        /*foldersViewModel.text.observe(viewLifecycleOwner, Observer {
            textView = it
        })*/
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}