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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.util.Locale
import java.util.TimeZone

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
                    saveProfilePictureUri(selectedUri)
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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

        loadUserProfile()

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

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        emailTextView.text = user.email

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (isAdded && document != null && document.exists()) {
                val rawName = document.getString("name") ?: "N/A"
                val formattedName = rawName.trim().lowercase(Locale.getDefault())
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                nameTextView.text = formattedName
                
                val currency = document.getString("currency") ?: "USD"
                currencyTextView.text = currency
                
                val timezone = document.getString("timezone") ?: TimeZone.getDefault().id
                timezoneTextView.text = timezone
                
                val profilePictureUrl = document.getString("profilePictureUrl")
                
                if (!profilePictureUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(profilePictureUrl)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .transform(CircleTransform())
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_account_circle)
                }
            }
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset All Data?")
            .setMessage("This will reset your wallet balance to $0 and delete all transaction history and favorites. This action cannot be undone.")
            .setPositiveButton("Reset Everything") { _, _ -> resetAllWalletData() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetAllWalletData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        userRef.update("balance", 0.0)

        userRef.collection("watchlist").get().addOnSuccessListener { snapshots ->
            for (doc in snapshots) {
                doc.reference.delete()
            }
        }

        userRef.collection("transactions").get().addOnSuccessListener { snapshots ->
            for (doc in snapshots) {
                doc.reference.delete()
            }
            if (isAdded) Toast.makeText(context, "Wallet and data reset successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCurrencySelectionDialog() {
        if (!isAdded) return
        val currencies = arrayOf("USD ($)", "EUR (€)", "ILS (₪)", "GBP (£)", "JPY (¥)")
        val currencyCodes = arrayOf("USD", "EUR", "ILS", "GBP", "JPY")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setItems(currencies) { _, which ->
                val selectedCode = currencyCodes[which]
                saveCurrency(selectedCode)
            }
            .show()
    }

    private fun saveCurrency(currencyCode: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("currency", currencyCode)
            .addOnSuccessListener {
                if (isAdded) {
                    currencyTextView.text = currencyCode
                    Toast.makeText(context, "Currency updated to $currencyCode", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showTimezoneSelectionDialog() {
        if (!isAdded) return
        val timezones = arrayOf("UTC", "Israel (GMT+2/3)", "London (GMT+0/1)", "New York (EST/EDT)", "Tokyo (JST)", "Dubai (GST)")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Timezone")
            .setItems(timezones) { _, which ->
                val tzIds = arrayOf("UTC", "Asia/Jerusalem", "Europe/London", "America/New_York", "Asia/Tokyo", "Asia/Dubai")
                saveTimezone(tzIds[which])
            }
            .show()
    }

    private fun saveTimezone(tzId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("timezone", tzId)
            .addOnSuccessListener {
                if (isAdded) {
                    timezoneTextView.text = tzId
                    Toast.makeText(context, "Timezone updated", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveProfilePictureUri(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("profilePictureUrl", uri.toString())
            .addOnSuccessListener { 
                if (isAdded) {
                    Picasso.get()
                        .load(uri)
                        .transform(CircleTransform())
                        .into(profileImageView)
                }
            }
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
                    val userId = auth.currentUser?.uid ?: return@setPositiveButton
                    db.collection("users").document(userId).update("name", newName)
                        .addOnSuccessListener { 
                            if (isAdded) {
                                (activity as? MainActivity)?.updateToolbarUsername(newName)
                                loadUserProfile()
                            }
                        }
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
                    reauthenticateAndChangePassword(currentPwd, newPwd)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reauthenticateAndChangePassword(currentPwd: String, newPwd: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPwd)

        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(newPwd).addOnCompleteListener {
                    if (isAdded) Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Re-authenticate")
            .setView(passwordEditText)
            .setPositiveButton("Confirm") { _, _ ->
                val password = passwordEditText.text.toString()
                val user = auth.currentUser ?: return@setPositiveButton
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if(isAdded && reauthTask.isSuccessful) deleteUserAccount()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        db.collection("users").document(userId).delete()
            .addOnCompleteListener { firestoreTask ->
                if (firestoreTask.isSuccessful) {
                    user.delete().addOnCompleteListener { 
                        val intent = Intent(activity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        activity?.finish()
                    }
                }
            }
    }
}
