package com.example.marketwatch

import android.os.Bundle
import android.util.Log
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.marketwatch.data.UserRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView

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
        
        val changePictureBtn: MaterialButton = view.findViewById(R.id.changePictureButton)

        // Force reset the listener to ensure it doesn't open the gallery
        changePictureBtn.setOnClickListener(null)
        changePictureBtn.setOnClickListener {
            Log.d("ProfileFragment", "!!!! BUTTON CLICKED - SHOWING DIALOG !!!!")
            showChangePictureUrlDialog()
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

    private fun showChangePictureUrlDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter Image URL here..."
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Set Profile Picture URL")
            .setMessage("Paste a direct link to an image from the web (e.g., Imgur, Pinterest, etc.)")
            .setView(editText)
            .setPositiveButton("Save URL") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    Log.d("ProfileFragment", "Updating URL to: $url")
                    viewModel.updateProfilePictureUrl(url)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                emailTextView.text = it.email
                nameTextView.text = it.name
                
                if (!it.profilePictureUrl.isNullOrEmpty()) {
                    Log.d("ProfileFragment", "Loading image: ${it.profilePictureUrl}")
                    Picasso.get()
                        .load(it.profilePictureUrl)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .transform(CircleTransform())
                        .into(profileImageView)
                }
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
