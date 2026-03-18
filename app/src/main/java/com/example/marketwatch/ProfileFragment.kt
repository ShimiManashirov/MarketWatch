package com.example.marketwatch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.marketwatch.data.UserRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                // Upload to Imgur (Free, no credit card)
                viewModel.uploadProfilePictureToImgur(requireContext(), imageUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val repository = UserRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)

        profileImageView = view.findViewById(R.id.profileImageView)
        nameTextView = view.findViewById(R.id.profileNameTextView)
        emailTextView = view.findViewById(R.id.profileEmailTextView)
        
        view.findViewById<MaterialButton>(R.id.changePictureButton).setOnClickListener {
            showPictureOptionsDialog()
        }

        view.findViewById<MaterialButton>(R.id.changeNameButton).setOnClickListener { showChangeNameDialog() }
        view.findViewById<MaterialButton>(R.id.changePasswordButton).setOnClickListener { showChangePasswordDialog() }
        view.findViewById<MaterialButton>(R.id.myPostsButton).setOnClickListener { 
            findNavController().navigate(R.id.action_profile_to_userPosts)
        }
        view.findViewById<MaterialButton>(R.id.resetWalletButton).setOnClickListener { showResetConfirmationDialog() }
        view.findViewById<MaterialButton>(R.id.deleteAccountButton).setOnClickListener { showDeleteAccountConfirmationDialog() }

        observeViewModel()
        return view
    }

    private fun showPictureOptionsDialog() {
        val options = arrayOf("Pick from Gallery", "Enter Image URL", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Set Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> showChangePictureUrlDialog()
                }
            }
            .show()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun showChangePictureUrlDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter Image URL here..."
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Set Profile Picture URL")
            .setView(editText)
            .setPositiveButton("Save URL") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) viewModel.updateProfilePictureUrl(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                emailTextView.text = it.email
                nameTextView.text = it.name
                ImageManager.loadProfileImage(profileImageView, it.profilePictureUrl)
            }
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showChangeNameDialog() {
        val editText = EditText(context).apply { setText(nameTextView.text.toString()) }
        AlertDialog.Builder(requireContext())
            .setTitle("Change Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) viewModel.updateName(newName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Data?")
            .setMessage("All transactions will be deleted.")
            .setPositiveButton("Reset") { _, _ -> viewModel.resetWalletData() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 40, 50, 10) }
        val currentPwd = EditText(context).apply { hint = "Current Password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val newPwd = EditText(context).apply { hint = "New Password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        layout.addView(currentPwd); layout.addView(newPwd)
        AlertDialog.Builder(requireContext()).setTitle("Change Password").setView(layout).setPositiveButton("Update") { _, _ ->
            if (currentPwd.text.isNotEmpty()) viewModel.updatePassword(currentPwd.text.toString(), newPwd.text.toString())
        }.show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Delete Account?").setPositiveButton("Delete") { _, _ -> 
            val pwd = EditText(context).apply { hint = "Confirm Password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
            AlertDialog.Builder(requireContext()).setTitle("Final Step").setView(pwd).setPositiveButton("Delete Forever") { _, _ ->
                viewModel.deleteAccount(pwd.text.toString())
            }.show()
        }.show()
    }
}
