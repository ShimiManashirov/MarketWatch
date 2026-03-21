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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.marketwatch.data.UserRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.TimeZone

class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var currencyTextView: TextView
    private lateinit var timezoneTextView: TextView

    private val avatarUrls = listOf(
        "https://api.dicebear.com/7.x/avataaars/png?seed=Felix",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Aneka",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Boo",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Jasper",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Lucky",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Luna",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Max",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Milo",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Oliver",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Jack",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Toby",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Bella"
    )
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                viewModel.uploadProfilePicture(requireContext(), imageUri)
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
        currencyTextView = view.findViewById(R.id.profileCurrencyTextView)
        timezoneTextView = view.findViewById(R.id.profileTimezoneTextView)
        
        // Listeners
        view.findViewById<MaterialButton>(R.id.changePictureButton).setOnClickListener { showAvatarPickerContainer() }
        view.findViewById<MaterialButton>(R.id.changeNameButton).setOnClickListener { showChangeNameDialog() }
        view.findViewById<MaterialButton>(R.id.changePasswordButton).setOnClickListener { showChangePasswordDialog() }
        view.findViewById<MaterialButton>(R.id.myPostsButton).setOnClickListener { 
            findNavController().navigate(R.id.action_profile_to_userPosts)
        }
        
        // Fix for the reported buttons
        view.findViewById<MaterialButton>(R.id.setCurrencyButton).setOnClickListener { showCurrencyDialog() }
        view.findViewById<MaterialButton>(R.id.setTimezoneButton).setOnClickListener { showTimezoneDialog() }
        view.findViewById<MaterialButton>(R.id.resetWalletButton).setOnClickListener { showResetConfirmationDialog() }
        view.findViewById<MaterialButton>(R.id.deleteAccountButton).setOnClickListener { showDeleteAccountConfirmationDialog() }

        observeViewModel()
        return view
    }

    private fun showCurrencyDialog() {
        val currencies = arrayOf("USD", "EUR", "GBP", "ILS", "JPY")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setItems(currencies) { _, which ->
                viewModel.updateCurrency(currencies[which])
            }
            .show()
    }

    private fun showTimezoneDialog() {
        val timezones = arrayOf("UTC", "Israel", "GMT", "CET", "EST", "PST")
        val ids = arrayOf("UTC", "Asia/Jerusalem", "GMT", "CET", "EST", "PST")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Timezone")
            .setItems(timezones) { _, which ->
                viewModel.updateTimezone(ids[which])
            }
            .show()
    }

    private fun showAvatarPickerContainer() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_avatar_picker, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.avatarRecyclerView)
        val btnCustom = dialogView.findViewById<MaterialButton>(R.id.btnCustomFromGallery)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Select Avatar")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = object : RecyclerView.Adapter<AvatarViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_avatar, parent, false)
                return AvatarViewHolder(v)
            }

            override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
                val url = avatarUrls[position]
                val iv = holder.itemView.findViewById<ImageView>(R.id.avatarImageView)
                ImageManager.loadProfileImage(iv, url)
                holder.itemView.setOnClickListener {
                    viewModel.updateProfilePictureUrl(url)
                    dialog.dismiss()
                }
            }

            override fun getItemCount() = avatarUrls.size
        }

        btnCustom.setOnClickListener {
            pickImageFromGallery()
            dialog.dismiss()
        }

        dialog.show()
    }

    private class AvatarViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                emailTextView.text = it.email
                nameTextView.text = it.name
                currencyTextView.text = it.currency
                timezoneTextView.text = it.timezone
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
