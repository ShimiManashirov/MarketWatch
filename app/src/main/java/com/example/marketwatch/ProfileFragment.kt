package com.example.marketwatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
    private lateinit var currencyTextView: TextView
    private lateinit var timezoneTextView: TextView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedUri = result.data?.data
            if (selectedUri != null) {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireActivity().contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
                    viewModel.updateProfilePicture(selectedUri)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    if(isAdded) Toast.makeText(context, "Failed to get permission for the image.", Toast.LENGTH_LONG).show()
                }
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
        
        val changePictureButton: MaterialButton = view.findViewById(R.id.changePictureButton)
        val changeNameButton: MaterialButton = view.findViewById(R.id.changeNameButton)
        val changePasswordButton: MaterialButton = view.findViewById(R.id.changePasswordButton)
        val myPostsButton: MaterialButton = view.findViewById(R.id.myPostsButton)
        val setCurrencyButton: MaterialButton = view.findViewById(R.id.setCurrencyButton)
        val setTimezoneButton: MaterialButton = view.findViewById(R.id.setTimezoneButton)
        val resetWalletButton: MaterialButton = view.findViewById(R.id.resetWalletButton)
        val deleteAccountButton: MaterialButton = view.findViewById(R.id.deleteAccountButton)

        observeViewModel()

        changePictureButton.setOnClickListener { 
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { 
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*" 
            }
            pickImageLauncher.launch(intent)
        }

        changeNameButton.setOnClickListener { showChangeNameDialog() }
        changePasswordButton.setOnClickListener { showChangePasswordDialog() }
        myPostsButton.setOnClickListener { 
            startActivity(Intent(activity, UserPostsActivity::class.java))
        }
        setCurrencyButton.setOnClickListener { showCurrencySelectionDialog() }
        setTimezoneButton.setOnClickListener { showTimezoneSelectionDialog() }
        resetWalletButton.setOnClickListener { showResetConfirmationDialog() }
        deleteAccountButton.setOnClickListener { showDeleteAccountConfirmationDialog() }

        return view
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                emailTextView.text = it.email
                val formattedName = it.name.trim().lowercase(Locale.getDefault())
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                nameTextView.text = formattedName
                currencyTextView.text = it.currency
                timezoneTextView.text = it.timezone
                
                if (!it.profilePictureUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(it.profilePictureUrl)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .transform(CircleTransform())
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_account_circle)
                }

                // Sync name with toolbar if needed
                (activity as? MainActivity)?.updateToolbarUsername(it.name)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // In the future, you could show a ProgressBar here
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.resetOperationSuccess()
            }
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset All Data?")
            .setMessage("This will reset your wallet balance to $0 and delete all transaction history and favorites. This action cannot be undone.")
            .setPositiveButton("Reset Everything") { _, _ -> viewModel.resetWalletData() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCurrencySelectionDialog() {
        if (!isAdded) return
        val currencies = arrayOf("USD ($)", "EUR (€)", "ILS (₪)", "GBP (£)", "JPY (¥)")
        val currencyCodes = arrayOf("USD", "EUR", "ILS", "GBP", "JPY")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setItems(currencies) { _, which ->
                viewModel.updateCurrency(currencyCodes[which])
            }
            .show()
    }

    private fun showTimezoneSelectionDialog() {
        if (!isAdded) return
        val timezones = arrayOf("UTC", "Israel (GMT+2/3)", "London (GMT+0/1)", "New York (EST/EDT)", "Tokyo (JST)", "Dubai (GST)")
        val tzIds = arrayOf("UTC", "Asia/Jerusalem", "Europe/London", "America/New_York", "Asia/Tokyo", "Asia/Dubai")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Timezone")
            .setItems(timezones) { _, which ->
                viewModel.updateTimezone(tzIds[which])
            }
            .show()
    }

    private fun showChangeNameDialog() {
        if(!isAdded) return
        val currentName = nameTextView.text.toString()
        val editText = EditText(context).apply { 
            setText(currentName)
            setSelection(text.length)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Change Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateName(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        if (!isAdded) return
        
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val currentPasswordET = EditText(context).apply {
            hint = "Current Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val newPasswordET = EditText(context).apply {
            hint = "New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmPasswordET = EditText(context).apply {
            hint = "Confirm New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(currentPasswordET)
        layout.addView(newPasswordET)
        layout.addView(confirmPasswordET)

        AlertDialog.Builder(context)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val currentPwd = currentPasswordET.text.toString()
                val newPwd = newPasswordET.text.toString()
                val confirmPwd = confirmPasswordET.text.toString()

                if (currentPwd.isNotEmpty() && newPwd == confirmPwd) {
                    viewModel.updatePassword(currentPwd, newPwd)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        if(!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ -> showReauthenticationDialog() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReauthenticationDialog() {
        if(!isAdded) return
        val passwordEditText = EditText(context).apply {
            hint = "Enter password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Re-authenticate")
            .setView(passwordEditText)
            .setPositiveButton("Confirm") { _, _ ->
                val password = passwordEditText.text.toString()
                if (password.isNotEmpty()) {
                    viewModel.deleteAccount(password)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
