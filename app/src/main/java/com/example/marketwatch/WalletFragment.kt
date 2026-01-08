package com.example.marketwatch

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class WalletFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var balanceTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wallet, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        balanceTextView = view.findViewById(R.id.walletBalanceTextView)
        val addFundsButton: MaterialButton = view.findViewById(R.id.addFundsButton)
        val withdrawFundsButton: MaterialButton = view.findViewById(R.id.withdrawFundsButton)

        loadBalance()

        addFundsButton.setOnClickListener { showFundsDialog(true) }
        withdrawFundsButton.setOnClickListener { showFundsDialog(false) }

        return view
    }

    private fun loadBalance() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !isAdded) return@addSnapshotListener
                val balance = snapshot.getDouble("balance") ?: 0.0
                balanceTextView.text = "$${String.format("%.2f", balance)}"
            }
    }

    private fun showFundsDialog(isAdding: Boolean) {
        val input = EditText(requireContext()).apply {
            hint = "Enter amount"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isAdding) "Add Funds" else "Withdraw Funds")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val amountText = input.text.toString()
                if (amountText.isNotEmpty()) {
                    val amount = amountText.toDouble()
                    updateBalance(if (isAdding) amount else -amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateBalance(delta: Double) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentBalance = snapshot.getDouble("balance") ?: 0.0
            val newBalance = currentBalance + delta

            if (newBalance < 0) {
                throw Exception("Insufficient funds")
            }

            transaction.update(userRef, "balance", newBalance)
        }.addOnSuccessListener {
            Toast.makeText(context, "Transaction successful", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Transaction failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
